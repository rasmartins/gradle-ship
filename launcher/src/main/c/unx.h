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

#ifndef UNIX_H_INCLUDED_
#define UNIX_H_INCLUDED_

#ifndef _DEFAULT_SOURCE
#  define _DEFAULT_SOURCE
#endif

#ifndef _BSD_SOURCE
#  define _BSD_SOURCE
#endif

#ifndef _XOPEN_SOURCE
#  define _XOPEN_SOURCE 1000
#endif

#define PATH_SEP '/'

#include <stdbool.h>
#include <errno.h>

#include <fcntl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

#if defined(__linux__)
#include <linux/limits.h>
#elif defined(__APPLE__)
#include <sys/syslimits.h>
#include <mach-o/dyld.h>
#endif

/**
 * Find the path to the current executable.
 *
 * @param[in,out] bfr buffer to hold the path.
 * @param[in] bfr_size size of the buffer in bytes.
 *
 * @return number of bytes written to the input buffer
 */
static inline int
ship_find_exe_path(char* bfr, size_t bfr_size)
{
#if defined(__linux__)
  ssize_t rv = readlink("/proc/self/exe", bfr, bfr_size - 1);
  if (rv < 0)
    rv = 0;
  return rv;

#elif defined(__APPLE__)
  unsigned size = bfr_size;
  _NSGetExecutablePath(bfr, &size);
  return size;
#endif
}

static inline bool
ship_file_exists(const char* path)
{
  struct stat ss;
  int rv = lstat(path, &ss);
  if (rv != 0)
    return false;

  return S_ISREG(ss.st_mode);
}

void
ship_execute(const char* program, char** argv, int argc)
{
  pid_t childPid = fork();
  if (childPid == 0)
  {
    if (execvp(program, argv) < 0)
    {
      fprintf(stderr, "ERROR: failed to spawn process: %s: %s\n", program, strerror(errno));
      exit(1);
    }
  }
  else
  {
    int status = 0;
    waitpid(childPid, &status, 0);
  }
}

#endif
