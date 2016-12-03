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

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#if defined(__linux__) || defined(__APPLE__)
#  include "unx.h"
#elif defined(_WIN32)
#  include "win.h"
#endif

void
die(const char* message)
{
  fprintf(stderr, "ERROR: %s\n", message);
  exit(1);
}

/**
 * Find executable folder.
 */
static inline char*
ship_find_base_folder(void)
{
  char* path = calloc(PATH_MAX, 1);
  int pathSize = ship_find_exe_path(path, PATH_MAX);
  if (pathSize <= 0)
    die("failed to find executable path");

  for (int i = pathSize - 1; i >= 0; --i)
  {
    char c = path[i];
    path[i] = '\0';
    if (c == PATH_SEP)
      break;
  }

  return path;
}

/**
 * Find JRE executable.
 */
static inline char*
ship_find_jvm_path(const char* baseFolder)
{
  char* jvmPath = calloc(PATH_MAX, 1);

  sprintf(jvmPath, "%s/jvm/bin/%s", baseFolder, "javaw.exe");
  if (ship_file_exists(jvmPath))
    return jvmPath;

  sprintf(jvmPath, "%s/jvm/bin/%s", baseFolder, "java");
  if (ship_file_exists(jvmPath))
    return jvmPath;

  die("failed to find Java Runtime Environment");
  return NULL;
}

/**
 * Find launcher.jar
 */
static inline char*
ship_find_launcher_jar_path(const char* baseFolder)
{
  char* path = calloc(PATH_MAX, 1);

  sprintf(path, "%s/launcher.jar", baseFolder);
  if (ship_file_exists(path))
    return path;

  die("failed to find launcher.jar");
  return NULL;
}

int
main(int argc, char** argv)
{
  if (argc < 2)
  {
    fprintf(stderr, "Usage: %s [args]\n", argv[0]);
    return 1;
  }

  char* baseFolder = ship_find_base_folder();
  char* jvmPath = ship_find_jvm_path(baseFolder);

  // Array of arguments.
  char** args = calloc(512, sizeof(char*));
  int argIndex = 0;

  // Path.
  args[argIndex++] = jvmPath;
  args[argIndex++] = "-jar";
  args[argIndex++] = ship_find_launcher_jar_path(baseFolder);

  // Path to JVM executable.
  args[argIndex++] = jvmPath;

  // Application root folder.
  args[argIndex++] = baseFolder;

  // Extra arguments.
  for (int i = 1; i < argc; ++i)
    args[argIndex++] = strdup(argv[i]);

  args[argIndex] = NULL;

  const char* debug = getenv("SHIP_LAUNCHER_DEBUG");
  if (debug)
  {
    fprintf(stderr, "ship: executing:");
    for (int i = 0; i < argIndex; ++i)
      fprintf(stderr, " '%s'", args[i]);
    fprintf(stderr, "\n");
  }

  ship_execute(jvmPath, args, argIndex);

  return 0;
}
