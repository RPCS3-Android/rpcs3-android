
#include "iso.hpp"
#include "Utilities/File.h"
#include "util/types.hpp"
#include <cctype>
#include <cstddef>
#include <cstdint>
#include <cstdio>
#include <ctime>
#include <filesystem>
#include <memory>

bool iso_fs::initialize() {
  constexpr std::size_t primaryVolumeDescOffset = 16;
  ensure(m_dev->block_size() >= sizeof(iso::VolumeHeader));
  std::vector<std::byte> block(m_dev->block_size());

  for (std::size_t i = 0; i < 256; ++i) {
    if (m_dev->read(primaryVolumeDescOffset + i, block.data(), 1) != 1) {
      break;
    }

    auto header = reinterpret_cast<iso::VolumeHeader *>(block.data());

    if (header->type == 255) {
      break;
    }

    if (header->type != 1 ||
        std::memcmp(header->standard_id, "CD001", 5) != 0) {
      continue;
    }

    auto pvd = reinterpret_cast<iso::PrimaryVolumeDescriptor *>(block.data());
    m_root_dir = pvd->root;
    return true;
  }

  return false;
}

fs::file iso_fs::open(const std::filesystem::path &path, fs::open_mode mode) {
  if (mode != fs::open_mode::read) {
    return {};
  }

  auto optEntry = open_entry(path, false);
  if (!optEntry) {
    return {};
  }

  return read_file(*optEntry);
}

fs::dir iso_fs::open_dir(const std::filesystem::path &path) {
  auto optEntry = open_entry(path, true);
  if (!optEntry) {
    return {};
  }

  auto items = read_dir(*optEntry);
  std::vector<fs::dir_entry> result_items(items.first.size());

  for (std::size_t i = 0; i < result_items.size(); ++i) {
    result_items[i] = items.first[i].to_fs_entry(items.second[i]);
  }

  fs::dir result;
  result.reset(std::make_unique<fs::virtual_dir>(std::move(result_items)));
  return result;
}

std::optional<iso::DirEntry>
iso_fs::open_entry(const std::filesystem::path &path, bool isDir) {
  auto pathString = std::filesystem::weakly_canonical(path).string();

  if (pathString == "/" || pathString == "\\" || pathString.empty()) {
    return m_root_dir;
  }

  auto item = m_root_dir;
  auto pathView = std::string_view(pathString);

  auto isStringEqNoCase = [](std::string_view lhs, std::string_view rhs) {
    if (lhs.size() != rhs.size()) {
      return false;
    }

    for (std::size_t i = 0; i < lhs.size(); ++i) {
      if (std::tolower(lhs[i]) != std::tolower(rhs[i])) {
        return false;
      }
    }

    return true;
  };

  while (!pathView.empty()) {
    auto sepPos = pathView.find_first_of("/\\");
    if (sepPos == 0) {
      pathView.remove_prefix(1);

      if (pathView.empty() && !isDir) {
        return {};
      }

      continue;
    }

    if ((item.flags & iso::DirEntryFlags::Directory) !=
        iso::DirEntryFlags::Directory) {
      return {};
    }

    auto dirName = pathView.substr(0, sepPos);
    if (sepPos == std::string_view::npos) {
      pathView = {};
    } else {
      pathView.remove_prefix(sepPos + (isDir ? 1 : 0));
    }

    auto items = read_dir(item);

    bool found = false;
    for (std::size_t i = 0; i < items.first.size(); ++i) {
      if (!isStringEqNoCase(items.second[i], dirName)) {
        continue;
      }

      item = items.first[i];
      found = true;
      break;
    }

    if (!found) {
      return {};
    }
  }

  if (isDir) {
    if ((item.flags & iso::DirEntryFlags::Directory) !=
        iso::DirEntryFlags::Directory) {
      return {};
    }
  } else {
    if ((item.flags & iso::DirEntryFlags::Directory) ==
        iso::DirEntryFlags::Directory) {
      return {};
    }
  }

  return item;
}

std::pair<std::vector<iso::DirEntry>, std::vector<std::string>>
iso_fs::read_dir(const iso::DirEntry &entry) {
  if ((entry.flags & iso::DirEntryFlags::Directory) !=
      iso::DirEntryFlags::Directory) {
    return {};
  }

  auto block_size = m_dev->block_size();
  auto total_block_count = 1;

  std::vector<std::byte> buffer(total_block_count * block_size);
  auto first_block = entry.lba.value();

  std::vector<iso::DirEntry> isoEntries;
  std::vector<std::string> names;

  for (std::size_t block = first_block,
                   end = first_block + entry.length.value() / block_size;
       block < end;) {
    auto block_count = m_dev->read(block, buffer.data(), total_block_count);
    block += block_count;

    std::size_t buffer_offset = 0;
    std::size_t buffer_size = block_count * block_size;

    std::size_t count = 0;
    while (buffer_offset < buffer_size) {
      auto item = reinterpret_cast<const iso::DirEntry *>(buffer.data() +
                                                          buffer_offset);

      buffer_offset += item->entry_length;

      if (item->entry_length < sizeof(iso::DirEntry)) {
        buffer_offset += block_size;
        buffer_offset &= ~(block_size - 1);
        continue;
      }

      if (item->filename_length == 0 ||
          item->filename_length + sizeof(iso::DirEntry) > item->entry_length) {
        continue;
      }

      auto filename = std::string_view(reinterpret_cast<const char *>(item + 1),
                                       item->filename_length);

      if (filename.length() == 1) {
        // can be special name
        if (filename[0] == 0) {
          filename = ".";
        } else if (filename[0] == 1) {
          filename = "..";
        }
      }

      filename = filename.substr(0, filename.find_first_of(";\0\n"));
      if (filename.empty()) {
        continue;
      }

      isoEntries.push_back(*item);
      names.emplace_back(filename);
    }
  }

  return {std::move(isoEntries), std::move(names)};
}

fs::file iso_fs::read_file(const iso::DirEntry &entry) {
  if ((entry.flags & iso::DirEntryFlags::Directory) ==
      iso::DirEntryFlags::Directory) {
    return {};
  }

  if (entry.length.value() == 0) {
    return fs::make_stream<std::vector<std::uint8_t>>();
  }

  auto block_size = m_dev->block_size();
  auto block_count = (entry.length.value() + block_size - 1) / block_size;

  std::vector<std::uint8_t> data;
  data.resize(block_count * block_size);
  if (!m_dev->read(entry.lba.value(), data.data(), block_count)) {
    return {};
  }

  data.resize(entry.length.value());
  return fs::make_stream(std::move(data), entry.to_fs_stat());
}
