/*
 * @(#) net/sf/jadretro/ConstUtfContent.java --
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UTFDataFormatException;

final class ConstUtfContent extends ConstPoolContent {

	private/* final */String value;

	ConstUtfContent(String value) {
		this.value = value;
	}

	ConstUtfContent(InputStream in) throws IOException {
		value = readUTF(in);
	}

	void writeTo(OutputStream out) throws IOException {
		writeUTF(out, value);
	}

	boolean isEqualTo(ConstPoolContent other) {
		return other instanceof ConstUtfContent
				&& value.equals(((ConstUtfContent) other).value);
	}

	String utfValue() {
		return value;
	}

	private static String readUTF(InputStream in) throws IOException {
		byte[] bytes = new byte[readUnsignedShort(in)];
		readFully(in, bytes);
		String str = decodeUTF(bytes);
		if (str == null)
			throw new UTFDataFormatException();
		return str;
	}

	private static String decodeUTF(byte[] bytes) {
		int len = bytes.length;
		StringBuffer sbuf = new StringBuffer(len);
		for (int i = 0; i < len; i++) {
			int c1 = bytes[i];
			if (c1 <= 0) {
				if (++i >= len)
					return null;
				int c2 = bytes[i];
				if ((c2 & 0xc0) != 0x80)
					return null;
				if ((c1 & 0xe0) == 0xc0) {
					c1 = ((c1 & 0x1f) << 6) | (c2 & 0x3f);
				} else {
					if (++i >= len || (c1 & 0xf0) != 0xe0)
						return null;
					int c3 = bytes[i];
					if ((c3 & 0xc0) != 0x80)
						return null;
					c1 = (c1 << 12) | ((c2 & 0x3f) << 6) | (c3 & 0x3f);
				}
			}
			sbuf.append((char) c1);
		}
		return sbuf.toString();
	}

	private static void writeUTF(OutputStream out, String str)
			throws IOException {
		int count = str.length();
		ByteArrayOutputStream baos = new ByteArrayOutputStream((count >> 1)
				+ count + 16);
		encodeUTF(baos, str);
		int len = baos.size();
		if (len > 0xffff)
			throw new UTFDataFormatException();
		writeShort(out, len);
		baos.writeTo(out);
	}

	private static void encodeUTF(ByteArrayOutputStream baos, String str) {
		int count = str.length();
		for (int i = 0; i < count; i++) {
			int c1 = str.charAt(i);
			if (c1 == 0 || c1 > 0x7f) {
				if (c1 > 0x7ff) {
					baos.write((c1 >> 12) | 0xe0);
					baos.write(((c1 >> 6) & 0x3f) | 0x80);
				} else {
					baos.write((c1 >> 6) | 0xc0);
				}
				c1 = (c1 & 0x3f) | 0x80;
			}
			baos.write(c1);
		}
	}
}
