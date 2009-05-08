/*
 * [The "BSD licence"]
 * Copyright (c) 2009 Ben Gruver
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.JesusFreke.dexlib;

import org.JesusFreke.dexlib.util.Output;
import org.JesusFreke.dexlib.util.Input;

public class OffsettedItemReference<T extends OffsettedItem<T>> extends
        ItemReference<T,OffsettedItemReference<T>> {
    private final CachedIntegerValueField underlyingField;

    public OffsettedItemReference(OffsettedSection<T> section, CachedIntegerValueField underlyingField) {
        super(section);
        this.underlyingField = underlyingField;
    }

    public OffsettedItemReference(DexFile dexFile, T item, CachedIntegerValueField underlyingField) {
        super(dexFile, item);
        this.underlyingField = underlyingField;
    }

    public OffsettedSection<T> getSection() {
        return (OffsettedSection<T>)super.getSection();
    }

    public void writeTo(Output out) {
        T item = getReference();
        if (item != null && !item.isPlaced()) {
            throw new RuntimeException("Trying to write reference to an item that hasn't been placed.");
        }
        
        /*if (out.getCursor() != underlyingField.getCachedValue()) {
            throw new RuntimeException("Trying to write a reference in a difference location from where it was placed");
        }*/
        //TODO: this is a hack to force it to reload the correct offset value
        if (item == null) {
            underlyingField.cacheValue(0);
        } else {
            underlyingField.cacheValue(item.getOffset());
        }

        underlyingField.writeTo(out);                          
    }

    public void readFrom(Input in) {
        underlyingField.readFrom(in);
        if (underlyingField.getCachedValue() != 0) {
            setReference(getSection().getByOffset(underlyingField.getCachedValue()));
        }
    }

    public int place(int offset) {
        if (getReference() != null) { 
            underlyingField.cacheValue(getReference().getOffset());
        } else {
            underlyingField.cacheValue(0);
        }
        return underlyingField.place(offset);
    }
}