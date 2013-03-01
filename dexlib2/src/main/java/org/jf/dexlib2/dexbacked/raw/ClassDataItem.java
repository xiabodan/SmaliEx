/*
 * Copyright 2013, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib2.dexbacked.raw;

import com.google.common.base.Joiner;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.dexbacked.DexReader;
import org.jf.dexlib2.util.AnnotatedBytes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class ClassDataItem {
    private abstract static class ClassDataItemAnnotator extends SectionAnnotator {
        public ClassDataItemAnnotator() {
        }

        @Nonnull @Override public String getItemName() {
            return "class_data_item";
        }

        @Override protected void annotateItem(@Nonnull AnnotatedBytes out, @Nonnull RawDexFile dexFile, int itemIndex) {
            DexReader reader = dexFile.readerAt(out.getCursor());

            int mark = reader.getOffset();
            int staticFieldsSize = reader.readSmallUleb128();
            out.annotate(reader.getOffset() - mark, "static_fields_size = %d", staticFieldsSize);

            mark = reader.getOffset();
            int instanceFieldsSize = reader.readSmallUleb128();
            out.annotate(reader.getOffset() - mark, "instance_fields_size = %d", instanceFieldsSize);

            mark = reader.getOffset();
            int directMethodsSize = reader.readSmallUleb128();
            out.annotate(reader.getOffset() - mark, "direct_methods_size = %d", directMethodsSize);

            mark = reader.getOffset();
            int virtualMethodsSize = reader.readSmallUleb128();
            out.annotate(reader.getOffset() - mark, "virtual_methods_size = %d", virtualMethodsSize);

            int previousIndex = 0;
            for (int i=0; i<staticFieldsSize; i++) {
                out.annotate(0, "static_field[%d]", i);
                out.indent();
                previousIndex = annotateEncodedField(out, dexFile, reader, previousIndex);
                out.deindent();
            }

            previousIndex = 0;
            for (int i=0; i<instanceFieldsSize; i++) {
                out.annotate(0, "instance_field[%d]", i);
                out.indent();
                previousIndex = annotateEncodedField(out, dexFile, reader, previousIndex);
                out.deindent();
            }

            previousIndex = 0;
            for (int i=0; i<directMethodsSize; i++) {
                out.annotate(0, "direct_method[%d]", i);
                out.indent();
                previousIndex = annotateEncodedMethod(out, dexFile, reader, previousIndex);
                out.deindent();
            }

            previousIndex = 0;
            for (int i=0; i<virtualMethodsSize; i++) {
                out.annotate(0, "virtual_method[%d]", i);
                out.indent();
                previousIndex = annotateEncodedMethod(out, dexFile, reader, previousIndex);
                out.deindent();
            }
        }

        private int annotateEncodedField(@Nonnull AnnotatedBytes out, @Nonnull RawDexFile dexFile,
                                          @Nonnull DexReader reader, int previousIndex) {
            int mark = reader.getOffset();
            int indexDelta = reader.readSmallUleb128();
            int fieldIndex = previousIndex + indexDelta;
            out.annotate(reader.getOffset() - mark, "field_idx_diff = %d: %s", indexDelta,
                    FieldIdItem.getReferenceAnnotation(dexFile, fieldIndex));

            mark = reader.getOffset();
            int accessFlags = reader.readSmallUleb128();
            out.annotate(reader.getOffset() - mark, "access_flags = 0x%x: %s", accessFlags,
                    Joiner.on('|').join(AccessFlags.getAccessFlagsForField(accessFlags)));

            return fieldIndex;
        }

        private int annotateEncodedMethod(@Nonnull AnnotatedBytes out, @Nonnull RawDexFile dexFile,
                                           @Nonnull DexReader reader, int previousIndex) {
            int mark = reader.getOffset();
            int indexDelta = reader.readSmallUleb128();
            int methodIndex = previousIndex + indexDelta;
            out.annotate(reader.getOffset() - mark, "method_idx_diff = %d: %s", indexDelta,
                    MethodIdItem.getReferenceAnnotation(dexFile, methodIndex));

            mark = reader.getOffset();
            int accessFlags = reader.readSmallUleb128();
            out.annotate(reader.getOffset() - mark, "access_flags = 0x%x: %s", accessFlags,
                    Joiner.on('|').join(AccessFlags.getAccessFlagsForMethod(accessFlags)));

            mark = reader.getOffset();
            int codeOffset = reader.readSmallUleb128();
            out.annotate(reader.getOffset() - mark, "code_item[0x%x]", codeOffset);

            return methodIndex;
        }
    }

    @Nonnull
    public static SectionAnnotator getAnnotator() {

        return new ClassDataItemAnnotator() {
            @Override
            public void annotateSection(@Nonnull AnnotatedBytes out, @Nonnull RawDexFile dexFile, int itemCount) {
                final Map<Integer, String> classTypeMap = ClassDefItem.getClassDataTypeMap(dexFile);

                SectionAnnotator annotator = new ClassDataItemAnnotator() {
                    @Nullable @Override
                    public String getItemIdentity(@Nonnull RawDexFile dexFile, int itemIndex, int itemOffset) {
                        if (classTypeMap != null) {
                            return classTypeMap.get(itemOffset);
                        }
                        return null;
                    }
                };
                annotator.annotateSection(out, dexFile, itemCount);
            }
        };
    }
}