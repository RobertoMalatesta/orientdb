package com.orientechnologies.common.directmemory;

public enum MemTrace {
  TEST,
  PAGE_PRE_ALLOCATION,
  ADD_NEW_PAGE_IN_DISK_CACHE,
  CHECK_FILE_STORAGE,
  LOAD_PAGE_FROM_DISK,
  COPY_PAGE_DURING_FLUSH,
  COPY_PAGE_DURING_EXCLUSIVE_PAGE_FLUSH,
  FILE_FLUSH,
  LOAD_WAL_PAGE,
  ADD_NEW_PAGE_IN_MEMORY_STORAGE,
  ALLOCATE_CHUNK_TO_WRITE_DATA_IN_BATCH,
  DWL_ALLOCATE_CHUNK,
  DWL_ALLOCATE_COMPRESSED_CHUNK,
  ALLOCATE_FIRST_WAL_BUFFER,
  ALLOCATE_SECOND_WAL_BUFFER,
}