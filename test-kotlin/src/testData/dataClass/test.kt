/*
 * Copyright 2000-2020 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package testData.dataClass

// classes: A

data class A(                  // coverage: FULL
    private val x: Int,
    val z: Int                 // coverage: FULL
) {
    fun foo() {
        println("Hello world") // coverage: FULL
    }
}

fun main() {
    val a = A(42, 42)
    println(a.z)
    a.foo()
}