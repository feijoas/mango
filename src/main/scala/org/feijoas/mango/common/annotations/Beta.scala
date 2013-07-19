/*
 * Copyright (C) 2013 The Mango Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * The code of this project is a port of (or wrapper around) the Guava-libraries.
 *    See http://code.google.com/p/guava-libraries/
 * 
 * @author Markus Schneider
 */
package org.feijoas.mango.common.annotations

import scala.annotation.meta.{beanGetter, beanSetter, field, getter, setter}

/** Signifies that a public API (public class, method or field) is subject to
 *  incompatible changes, or even removal, in a future release. An API bearing
 *  this annotation is exempt from any compatibility guarantees made by its
 *  containing library. Note that the presence of this annotation implies nothing
 *  about the quality or performance of the API in question, only the fact that
 *  it is not "API-frozen."
 *
 *  @author Markus Schneider
 *  @since 0.7 (copied from guava-libraries)
 */
@field @getter @setter @beanGetter @beanSetter
class Beta extends scala.annotation.StaticAnnotation