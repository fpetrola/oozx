/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package model.tags;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.concurrent.TimeUnit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Reaches the internet, so it fails on a train and says nothing about the code when it does.
 * <p>
 * The condition lives on the test rather than in surefire's configuration because a build is not
 * the only thing that runs tests: asking an IDE for "all tests in the module" ignores anything the
 * pom says. This way the test is off everywhere until someone asks for it by name.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
// The default timeout is set for the gate, where a minute means something has hung. These are
// the ones that legitimately take longer, which is why they are not in it.
@Timeout(value = 30, unit = TimeUnit.MINUTES)
@Tag("network")
@EnabledIfSystemProperty(named = "oozx.network", matches = "true", disabledReason = "run it with -Doozx.network=true")
public @interface NeedsNetwork {
}
