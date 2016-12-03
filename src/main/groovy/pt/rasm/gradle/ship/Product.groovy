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

package pt.rasm.gradle.ship

/**
 * Class representing a software product.
 */
class Product {
    /** Product identifier (alphanumeric, underscore, dash). */
    String id
    /** Product name (presented to the user. */
    String name
    /** Product version. */
    String version
    /** Product revision. */
    String revision
    /** Product vendor. */
    String vendor
    /** Product license file. */
    File license
    /** Product installation icon. */
    File installIcon
    /** Product installation header. */
    File installHeader
}
