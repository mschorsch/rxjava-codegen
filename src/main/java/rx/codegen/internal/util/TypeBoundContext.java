/*
 * Copyright 2015 Matthias Schorsch.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.codegen.internal.util;

/**
 *
 * @author Matthias
 */
enum TypeBoundContext implements Context {

    OPEN_BRACKET("<"),
    CLOSED_BRACKET(">"),
    COMMA(", "),
    ARRAY_BRACKETS("[]"),
    WILDCARD("?"),
    UPPER_BOUND(" extends "),
    LOWER_BOUND(" super "),
    INTERSECTION_BOUND(" & ");

    private final String boundname;

    private TypeBoundContext(String boundname) {
        this.boundname = boundname;
    }

    public String getBoundname() {
        return boundname;
    }

    @Override
    public String toString() {
        return boundname;
    }
}
