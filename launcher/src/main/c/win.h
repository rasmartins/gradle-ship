//******************************************************************************
// Copyright (C) 2016 Ricardo Martins                                          *
//******************************************************************************
// Licensed under the Apache License, Version 2.0 (the "License");             *
// you may not use this file except in compliance with the License. You may    *
// obtain a copy of the License at                                             *
//                                                                             *
// http://www.apache.org/licenses/LICENSE-2.0                                  *
//                                                                             *
// Unless required by applicable law or agreed to in writing, software         *
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT   *
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.            *
// See the License for the specific language governing permissions and         *
// limitations under the License.                                              *
//******************************************************************************

#ifndef WIN_H_INCLUDED_
#define WIN_H_INCLUDED_

#include <stdio.h>
#include <stdbool.h>

#include <process.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <windows.h>

#define PATH_SEP '\\'

static inline int
ship_find_exe_path(char* bfr, size_t bfr_size)
{
  return GetModuleFileName(NULL, bfr, bfr_size - 1);
}

static inline bool
ship_file_exists(const char* path)
{
  struct _stat ss;
  int rv = _stat(path, &ss);
  if (rv != 0)
    return false;

  return S_ISREG(ss.st_mode);
}

void
ship_execute(const char* program, char** argv, int argc)
{
  int rv = _spawnvp(P_OVERLAY, program, (const char* const*)argv);
  fprintf(stderr, "ERROR: failed to spawn process (%d)\n", rv);
}

#endif
