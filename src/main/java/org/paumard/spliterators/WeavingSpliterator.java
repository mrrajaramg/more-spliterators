/*
 * Copyright (C) 2015 José Paumard
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

package org.paumard.spliterators;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * @author José
 */
public class WeavingSpliterator<E> implements Spliterator<E> {

    private Spliterator<E>[] spliterators;
    private boolean[] spliteratorsFinished;
    private Deque<E> elements = new ArrayDeque<>();
    private boolean firstGroup = true;
    private boolean moreElements;

    public static <E> WeavingSpliterator<E> of(Spliterator<E>... spliterators) {
        Objects.requireNonNull(spliterators);
        if (spliterators.length < 2)
            throw new IllegalArgumentException("Cannot weave less than 2 spliterators");

        return new WeavingSpliterator<>(spliterators);
    }

    @SafeVarargs
    private WeavingSpliterator(Spliterator<E>... spliterators) {
        this.spliterators = spliterators;
        this.spliteratorsFinished = new boolean[spliterators.length];
    }

    private void consumeOneElementOnEachSpliterator() {
        Deque<E> elementsWave = new ArrayDeque<>();
        moreElements = true;
        for (int i = 0; i < spliterators.length && moreElements; i++) {
            moreElements = spliterators[i].tryAdvance(elementsWave::addLast);
        }
        if (moreElements) {
            elements.addAll(elementsWave);
        }
    }

    @Override
    public boolean tryAdvance(Consumer<? super E> action) {
        if (firstGroup) {
            consumeOneElementOnEachSpliterator();
            firstGroup = false;
        }
        if (!elements.isEmpty() && moreElements) {
            action.accept(elements.pop());
            return moreElements;
        }
        if (moreElements) {
            consumeOneElementOnEachSpliterator();
        }
        if (!elements.isEmpty() && moreElements) {
            action.accept(elements.pop());
            return moreElements;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Spliterator<E> trySplit() {
        Spliterator<E>[] spliterators = new Spliterator[this.spliterators.length];
        int index = 0;
        for (Spliterator<E> spliterator : this.spliterators) {
            spliterators[index++] = spliterator.trySplit();
        }
        return new WeavingSpliterator<>(spliterators);
    }

    @Override
    public long estimateSize() {
        int size = 0;
        for (Spliterator<E> spliterator : this.spliterators) {
            size += spliterator.estimateSize();
        }
        return size;
    }

    @Override
    public int characteristics() {
        return this.spliterators[0].characteristics();
    }
}