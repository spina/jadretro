/*
 * @(#) net/sf/jadretro/ConstantRef.java --
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

final class ConstantRef extends ClassEntity {

	private/* final */int index;

	private/* final */ClassFile classFile;

	ConstantRef(int index, ClassFile classFile) {
		this.index = index;
		this.classFile = classFile;
	}

	ConstantRef(InputStream in, ClassFile classFile, boolean isZeroAllowed)
			throws IOException {
		this.classFile = classFile;
		index = readUnsignedShort(in);
		if ((!isZeroAllowed && index == 0)
				|| classFile.getConstantPoolCount() <= index)
			throw new BadClassFileException();
	}

	static ConstantRef readAsByteFrom(InputStream in, ClassFile classFile)
			throws IOException {
		int index = readUnsignedByte(in);
		if (index == 0 || classFile.getConstantPoolCount() <= index)
			throw new BadClassFileException();
		return new ConstantRef(index, classFile);
	}

	boolean isWide() {
		return (index & ~0xff) != 0;
	}

	void writeAsByteTo(OutputStream out) throws IOException {
		if (isWide())
			throw new ClassOverflowException();
		out.write(index);
	}

	void writeTo(OutputStream out) throws IOException {
		writeShort(out, index);
	}

	private ConstantPoolEntry getConstantEntry() {
		return classFile.getConstantAt(index);
	}

	boolean isClassConst() {
		return getConstantEntry().isClassConst();
	}

	ConstantRef classOrName() throws BadClassFileException {
		return getConstantEntry().content().classOrName();
	}

	ConstantRef descriptor() throws BadClassFileException {
		return getConstantEntry().content().descriptor();
	}

	String utfValue() throws BadClassFileException {
		return getConstantEntry().content().utfValue();
	}

	String getEntityClassNameValue() throws BadClassFileException {
		return classOrName().classOrName().utfValue();
	}

	boolean isZero() {
		return index == 0;
	}

	boolean isEqualTo(ConstantRef other) {
		return other.index == index;
	}
}
