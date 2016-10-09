/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.cache.internal

import org.gradle.api.Action
import org.gradle.cache.internal.filelock.LockOptionsBuilder
import org.gradle.test.fixtures.file.TestFile
import spock.lang.Specification

class FixedExclusiveModeCrossProcessCacheAccessTest extends Specification {
    def file = new TestFile("some-file.lock")
    def lockManager = Mock(FileLockManager)
    def initAction = Mock(CacheInitializationAction)
    def onOpenAction = Mock(Action)
    def onCloseAction = Mock(Action)
    def cacheAccess = new FixedExclusiveModeCrossProcessCacheAccess("<cache>", file, LockOptionsBuilder.mode(FileLockManager.LockMode.Exclusive), lockManager, initAction, onOpenAction, onCloseAction)

    def "acquires lock then initializes cache and runs handler action on open"() {
        def lock = Mock(FileLock)

        when:
        cacheAccess.open()

        then:
        1 * lockManager.lock(file, _, _) >> lock

        then:
        1 * initAction.requiresInitialization(lock) >> false

        then:
        1 * onOpenAction.execute(lock)
        0 * _
    }

    def "runs handler action then releases lock on close"() {
        def lock = Mock(FileLock)

        given:
        lockManager.lock(file, _, _) >> lock
        cacheAccess.open()

        when:
        cacheAccess.close()

        then:
        1 * onCloseAction.execute(lock)

        then:
        1 * lock.close()
        0 * _
    }

    def "does not run handler on close when not open"() {
        expect: false
    }
}
