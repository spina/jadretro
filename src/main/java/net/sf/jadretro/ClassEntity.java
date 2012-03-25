/*
 * @(#) net/sf/jadretro/ClassEntity.java --
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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

abstract class ClassEntity {

	static Vector readAttributes(InputStream in, ClassFile classFile)
			throws IOException {
		int count = readUnsignedShort(in);
		Vector attributes = new Vector(count);
		while (count-- > 0) {
			attributes.addElement(new AttributeEntry(in, classFile));
		}
		return attributes;
	}

	static void readFully(InputStream in, byte[] bytes) throws IOException {
		int ofs = 0;
		int len = bytes.length;
		while (ofs < len) {
			int res = in.read(bytes, ofs, len - ofs);
			if (res < 0)
				throw new EOFException();
			ofs += res;
		}
	}

	static byte readByte(InputStream in) throws IOException {
		return ((byte) readUnsignedByte(in));
	}

	static int readUnsignedByte(InputStream in) throws IOException {
		int c1 = in.read();
		if (c1 < 0)
			throw new EOFException();
		return c1;
	}

	static short readShort(InputStream in) throws IOException {
		return ((short) readUnsignedShort(in));
	}

	static int readUnsignedShort(InputStream in) throws IOException {
		int c1 = in.read();
		int c2 = in.read();
		if (c2 < 0)
			throw new EOFException();
		return (c1 << 8) | c2;
	}

	static int readInt(InputStream in) throws IOException {
		int c1 = in.read();
		int c2 = in.read();
		int c3 = in.read();
		int c4 = in.read();
		if (c4 < 0)
			throw new EOFException();
		return (((((c1 << 8) | c2) << 8) | c3) << 8) | c4;
	}

	abstract void writeTo(OutputStream out) throws IOException;

	static void writeToForArray(Vector entries, OutputStream out)
			throws IOException {
		int count = entries.size();
		writeCheckedUShort(out, count);
		for (int i = 0; i < count; i++) {
			((ClassEntity) entries.elementAt(i)).writeTo(out);
		}
	}

	static void writeCheckedUShort(OutputStream out, int value)
			throws IOException {
		if ((value & ~0xffff) != 0)
			throw new ClassOverflowException();
		writeShort(out, value);
	}

	static void writeShort(OutputStream out, int value) throws IOException {
		out.write(value >> 8);
		out.write(value);
	}

	static void writeInt(OutputStream out, int value) throws IOException {
		out.write(value >> 24);
		out.write(value >> 16);
		out.write(value >> 8);
		out.write(value);
	}
}
