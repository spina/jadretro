/*
 * @(#) src/net/sf/jadretro/ConstantPoolEntry.java --
 * a part of JadRetro source.
 **
 * Copyright (C) 2007-2008 Ivan Maidanski <ivmai@mail.ru>
 * All rights reserved.
 */

/*
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 **
 * This software is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License (GPL) for more details.
 **
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library. Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 **
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module. An independent module is a module which is not derived from
 * or based on this library. If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */

package net.sf.jadretro;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

final class ConstantPoolEntry extends ClassEntity {

	static final int UTF8_TAG = 1;

	static final int INTEGER_TAG = 3;

	static final int FLOAT_TAG = 4;

	static final int LONG_TAG = 5;

	static final int DOUBLE_TAG = 6;

	static final int CLASS_TAG = 7;

	static final int STRING_TAG = 8;

	static final int FIELDREF_TAG = 9;

	static final int METHODREF_TAG = 10;

	static final int IFACEMETHOD_TAG = 11;

	static final int NAMETYPE_TAG = 12;

	static final ConstantPoolEntry EMPTY_ENTRY = new ConstantPoolEntry(0,
			new ConstIntContent(0));

	private/* final */int tag;

	private/* final */ConstPoolContent content;

	private ConstantPoolEntry(int tag, ConstPoolContent content) {
		this.tag = tag;
		this.content = content;
	}

	ConstantPoolEntry(InputStream in, ClassFile classFile) throws IOException {
		tag = readUnsignedByte(in);
		content = decodeContent(in, tag, classFile);
	}

	private static ConstPoolContent decodeContent(InputStream in, int tag,
			ClassFile classFile) throws IOException {
		if (tag == CLASS_TAG || tag == STRING_TAG)
			return new ConstClassStringContent(in, classFile);
		if (tag == FIELDREF_TAG || tag == METHODREF_TAG
				|| tag == IFACEMETHOD_TAG || tag == NAMETYPE_TAG)
			return new ConstFieldMethodNameType(in, classFile);
		if (tag == INTEGER_TAG || tag == FLOAT_TAG)
			return new ConstIntContent(in);
		if (tag == LONG_TAG || tag == DOUBLE_TAG)
			return new ConstLongContent(in);
		if (tag == UTF8_TAG)
			return new ConstUtfContent(in);
		throw new BadClassFileException();
	}

	static ConstantPoolEntry makeClassString(ConstantRef name, boolean isClass) {
		return new ConstantPoolEntry(isClass ? CLASS_TAG : STRING_TAG,
				new ConstClassStringContent(name));
	}

	static ConstantPoolEntry makeFieldNormMethod(ConstantRef classConst,
			ConstantRef descriptor, boolean isField) {
		return new ConstantPoolEntry(isField ? FIELDREF_TAG : METHODREF_TAG,
				new ConstFieldMethodNameType(classConst, descriptor));
	}

	static ConstantPoolEntry makeNameAndType(ConstantRef name,
			ConstantRef descriptor) {
		return new ConstantPoolEntry(NAMETYPE_TAG,
				new ConstFieldMethodNameType(name, descriptor));
	}

	static ConstantPoolEntry makeUtf(String value) {
		return new ConstantPoolEntry(UTF8_TAG, new ConstUtfContent(value));
	}

	void writeTo(OutputStream out) throws IOException {
		out.write(tag);
		content.writeTo(out);
	}

	boolean isEqualTo(ConstantPoolEntry other) {
		return other.tag == tag && content.isEqualTo(other.content);
	}

	boolean isClassConst() {
		return tag == CLASS_TAG;
	}

	boolean isLongOrDouble() {
		return tag == LONG_TAG || tag == DOUBLE_TAG;
	}

	boolean isNameAndType() {
		return tag == NAMETYPE_TAG;
	}

	ConstPoolContent content() {
		return content;
	}
}
