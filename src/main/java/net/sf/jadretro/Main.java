/*
 * @(#) net/sf/jadretro/Main.java --
 * a part of JadRetro source (the main class).
 **
 * Copyright (C) 2007-2012 Ivan Maidanski <ivmai@mail.ru>
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

/**
 * JadRetro - a Java decompiler helper.
 ***
 * JadRetro is a command-line utility that could help You to successfully
 * decompile Java classes created by the modern Java compilers (of Java 1.4,
 * Java 1.5 or later).
 ***
 * JadRetro operates by transforming the specified Java class files
 * (if needed) into ones which could be processed correctly by an old Java
 * decompiler (designed to work with classes of Java 1.3 or earlier).
 ***
 * JadRetro is not a decompiler itself, it is a class transformer helping
 * some old (but good) Java decompilers to convert more class files and/or
 * generate more correct source code.
 ***
 * See "jadretro.txt" file for more details and for the license agreement.
 */

package net.sf.jadretro;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;

public final class Main {

	private Main() {
	}

	private static void showHelp() {
		System.out.println("JadRetro v1.6.1 - a Java decompiler helper");
		System.out
				.println("Copyright (C) 2007-2012 Ivan Maidanski <ivmai@mail.ru>");
		System.out
				.println("This is free software. All rights reserved. No warranties.");
		System.out.println();
		System.out.println("Usage arguments:"
				+ " [options] <class_file_or_dir> [<class_file_or_dir2> ...]");
		System.out.println("Options:");
		System.out.println(" -b "
				+ " Keep all (do not process) Java bridge methods");
		System.out.println(" -l " + " Do not adjust local class names");
		System.out.println(" -c "
				+ " Change class version to Java 1.3 if greater");
		System.out.println(" -d <directory> "
				+ " Specify output base folder for the modified class files");
		System.out.println(" -q " + " Reduce the verbosity of the output");
		System.out.println("");
		System.out
				.println(" "
						+ " JadRetro is a command-line utility that could help You to successfully");
		System.out.println("decompile Java classes created"
				+ " by the modern Java compilers (of Java 1.4,");
		System.out.println("Java 1.5 or later).");
		System.out
				.println(" "
						+ " JadRetro operates by transforming the specified Java class files");
		System.out
				.println("(if needed) into ones which could be processed correctly by an old Java");
		System.out
				.println("decompiler (designed to work with classes of Java 1.3 or earlier).");
		System.out
				.println(" "
						+ " JadRetro is not a decompiler itself, it is a class transformer helping");
		System.out
				.println("some old (but good) Java decompilers to convert more class files and/or");
		System.out.println("generate more correct source code.");
		System.out
				.println(" " + " See \"jadretro.txt\" file for more details.");
	}

	public static final void main(String args[]) {
		if (args.length == 0 || args[0].equals("-h") || args[0].equals("-help")) {
			showHelp();
		} else {
			int res = intMain(args);
			if (res != 0) {
				System.exit(res);
			}
		}
	}

	private static int intMain(String args[]) {
		int i = 0;
		boolean keepBridgeMethods = false;
		boolean noAdjLocClassNames = false;
		boolean setOldVer = false;
		File outdir = null;
		boolean loud = true;
		do {
			if (args[i].equals("-b")) {
				keepBridgeMethods = true;
			} else if (args[i].equals("-l")) {
				noAdjLocClassNames = true;
			} else if (args[i].equals("-c")) {
				setOldVer = true;
			} else if (args[i].equals("-q")) {
				loud = false;
			} else {
				if (!args[i].equals("-d") || args.length - 1 == i
						|| outdir != null)
					break;
				outdir = new File(args[++i]);
			}
		} while (++i < args.length);
		Hashtable filePathSet = new Hashtable();
		Hashtable classNameSet = outdir != null ? new Hashtable() : null;
		int classesCount = 0;
		File outfile = null;
		try {
			while (i < args.length) {
				File infile = new File(args[i]);
				if (filePathSet.put(infile.getPath(), "") == null) {
					if (infile.isDirectory()) {
						String[] list = infile.list();
						if (list == null)
							break;
						i++;
						if (list.length > i) {
							String[] newArgs = new String[list.length
									+ args.length - i];
							System.arraycopy(args, i, newArgs, list.length,
									args.length - i);
							args = newArgs;
							i = 0;
						} else {
							i -= list.length;
						}
						for (int j = 0; j < list.length; j++) {
							args[i + j] = (new File(infile, list[j])).getPath();
						}
						i--;
					} else {
						FileInputStream in = new FileInputStream(infile);
						if (args[i].endsWith(".class")
								|| args[i].endsWith(".CLA")) {
							classesCount++;
							ClassFile classFile = new ClassFile(
									new BufferedInputStream(in));
							in.close();
							boolean isUniqueName = false;
							String className = classFile.className();
							if (classNameSet == null
									|| classNameSet.put(className, "") == null) {
								isUniqueName = true;
							}
							boolean isChanged = false;
							boolean isNewClassName = false;
							if (!noAdjLocClassNames
									&& fixLocalClassNames(classFile)) {
								isChanged = true;
								String newClassName = classFile.className();
								if (!newClassName.equals(className)) {
									isNewClassName = true;
									className = newClassName;
									if (isUniqueName
											&& classNameSet != null
											&& classNameSet.get(className) != null) {
										isUniqueName = false;
									}
								}
							}
							if ((!isNewClassName || isUniqueName)
									&& (process(classFile, setOldVer,
											keepBridgeMethods) || isChanged)) {
								if (isUniqueName) {
									outfile = infile;
									if (outdir != null) {
										outfile = new File(outdir,
												className.replace('/',
														File.separatorChar)
														+ ".class");
									} else if (isNewClassName) {
										String inFilePath = infile.getPath();
										int dotExtPos = inFilePath
												.lastIndexOf('.');
										if (inFilePath
												.lastIndexOf(File.separatorChar) >= dotExtPos) {
											dotExtPos = inFilePath.length();
										}
										String parent = infile.getParent();
										outfile = new File(
												parent != null ? new File(
														parent) : new File(
														infile, ".."),
												className.substring(className
														.lastIndexOf('/') + 1)
														+ inFilePath
																.substring(dotExtPos));
									}
									String dottedClassName = className.replace(
											'/', '.');
									try {
										ByteArrayOutputStream baos = new ByteArrayOutputStream();
										classFile.writeTo(baos);
										if (loud) {
											System.out
													.println("Class transformed: "
															+ dottedClassName);
										}
										String parent = outfile.getParent();
										if (parent != null) {
											(new File(parent)).mkdirs();
										}
										FileOutputStream out = new FileOutputStream(
												outfile);
										baos.writeTo(out);
										out.close();
										if (isNewClassName && outdir == null
												&& !infile.equals(outfile)) {
											infile.delete();
										}
									} catch (ClassOverflowException e) {
										System.err
												.println("Too big class ignored: "
														+ dottedClassName);
									}
									outfile = null;
								} else {
									System.err
											.println("Duplicate class file ignored: "
													+ args[i]);
								}
							}
						} else {
							in.close();
							System.err.println("Ignoring file: " + args[i]);
						}
					}
				}
				i++;
			}
		} catch (FileNotFoundException e) {
			if (outfile != null) {
				System.err.println("Error: cannot open file for writing: "
						+ outfile.getPath());
				return 6;
			}
			System.err.println("Error: file not found: " + args[i]);
			return 2;
		} catch (BadClassFileException e) {
			System.err.println("Error: invalid class file: " + args[i]);
			return 5;
		} catch (EOFException e) {
			System.err.println("Error: unexpected end of file: " + args[i]);
			return 4;
		} catch (IOException e) {
			if (outfile != null) {
				System.err.println("Error: cannot write file: "
						+ outfile.getPath());
				return 7;
			}
			System.err.println("Error: cannot read file: " + args[i]);
			return 3;
		}
		if (i < args.length) {
			System.err.println("Error: cannot list directory: " + args[i]);
			return 1;
		}
		if (classesCount != 0) {
			if (loud) {
				System.out.println("Done.");
			}
		} else {
			System.err.println("Error: no files processed!");
		}
		return 0;
	}

	private static boolean fixLocalClassNames(ClassFile classFile)
			throws BadClassFileException {
		boolean isChanged = false;
		for (int i = classFile.getConstantPoolCount() - 1; i > 0; i--) {
			ConstantPoolEntry entry = classFile.getConstantAt(i);
			if (entry.isClassConst()) {
				String className = adjustLocalClassName(entry.content()
						.classOrName().utfValue());
				if (className != null) {
					classFile.changeClassConstAt(i, className);
					isChanged = true;
				}
			} else if (entry.isNameAndType()) {
				String descriptorValue = adjustLocClassInDescriptor(entry
						.content().descriptor().utfValue());
				if (descriptorValue != null) {
					classFile.changeNameAndTypeConstAt(i, descriptorValue);
					isChanged = true;
				}
			}
		}
		for (int i = classFile.getFieldsCount() - 1; i >= 0; i--) {
			if (adjustLocClassInFieldMethod(classFile.getFieldAt(i), classFile)) {
				isChanged = true;
			}
		}
		for (int i = classFile.getMethodsCount() - 1; i >= 0; i--) {
			FieldMethodEntry method = classFile.getMethodAt(i);
			if (adjustLocClassInFieldMethod(method, classFile)) {
				isChanged = true;
			}
			AttrCodeContent codeContent = method.findCode();
			if (codeContent != null) {
				for (int j = codeContent.getAttributesCount() - 1; j >= 0; j--) {
					AttributeEntry entry = codeContent.getAttributeAt(j);
					if (entry.getNameValue().equals(
							AttrLocalVarsContent.nameValue())
							&& adjustLocClassInLocalVars(
									(AttrLocalVarsContent) entry.content(),
									classFile)) {
						isChanged = true;
					}
				}
			}
		}
		return isChanged;
	}

	private static boolean adjustLocClassInFieldMethod(
			FieldMethodEntry fieldMethod, ClassFile classFile)
			throws BadClassFileException {
		String descriptorValue = adjustLocClassInDescriptor(fieldMethod
				.descriptor().utfValue());
		if (descriptorValue == null)
			return false;
		fieldMethod.changeDescriptor(classFile.addUtfConst(descriptorValue));
		return true;
	}

	private static boolean adjustLocClassInLocalVars(
			AttrLocalVarsContent localVarsContent, ClassFile classFile)
			throws BadClassFileException {
		boolean isChanged = false;
		for (int i = localVarsContent.getVarsCount() - 1; i >= 0; i--) {
			LocalVariableDesc localVarDesc = localVarsContent.getVarDescAt(i);
			String descriptorValue = adjustLocClassInDescriptor(localVarDesc
					.descriptor().utfValue());
			if (descriptorValue != null) {
				localVarDesc.changeDescriptor(classFile
						.addUtfConst(descriptorValue));
				isChanged = true;
			}
		}
		return isChanged;
	}

	private static String adjustLocClassInDescriptor(String descriptorValue) {
		boolean isChanged = false;
		int pos = -1;
		while ((pos = descriptorValue.indexOf('L', pos + 1)) >= 0) {
			int startPos = pos + 1;
			pos = descriptorValue.indexOf(';', startPos);
			if (pos < 0)
				break;
			String className = adjustLocalClassName(descriptorValue.substring(
					startPos, pos));
			if (className != null) {
				descriptorValue = descriptorValue.substring(0, startPos)
						+ className + descriptorValue.substring(pos);
				pos = startPos + className.length();
				isChanged = true;
			}
		}
		return isChanged ? descriptorValue : null;
	}

	private static String adjustLocalClassName(String className) {
		int pos = className.lastIndexOf('/') + 1;
		boolean isChanged = false;
		while ((pos = className.indexOf('$', pos + 1)) >= 0) {
			int len = className.length();
			while (++pos < len) {
				char ch = className.charAt(pos);
				if (!isAsciiDigit(ch) && ch != '$')
					break;
			}
			if (pos == len)
				break;
			if (className.charAt(pos - 1) != '$') {
				className = className.substring(0, pos) + "$"
						+ className.substring(pos);
				isChanged = true;
				pos++;
			}
		}
		return isChanged ? className : null;
	}

	private static boolean isAsciiDigit(char ch) {
		return ch >= '0' && ch <= '9';
	}

	private static boolean process(ClassFile classFile, boolean setOldVer,
			boolean keepBridgeMethods) throws BadClassFileException {
		boolean isChanged = false;
		int javaVer = classFile.getJavaVer();
		if (setOldVer && javaVer > 3) {
			javaVer = 3;
			classFile.setJavaVer(javaVer);
			isChanged = true;
		}
		String className = classFile.className();
		String superClassName = null;
		int assertDisabledFieldInd = -1;
		Hashtable usedStaticFields = null;
		boolean isAnonymousClass = false;
		int innerNamePos = className.lastIndexOf('$', className.length() - 2);
		if (innerNamePos >= 0 && className.lastIndexOf('/') + 1 < innerNamePos) {
			int pos = className.length();
			while (--pos > innerNamePos) {
				if (!isAsciiDigit(className.charAt(pos)))
					break;
			}
			if (innerNamePos == pos) {
				isAnonymousClass = true;
			}
			superClassName = classFile.getSuperClassName();
			assertDisabledFieldInd = classFile.findField("$assertionsDisabled",
					"Z", true);
			if (assertDisabledFieldInd >= 0
					&& !classFile.getFieldAt(assertDisabledFieldInd)
							.accessFlags().isFinal()) {
				assertDisabledFieldInd = -1;
			}
		} else {
			usedStaticFields = new Hashtable(
					(classFile.getFieldsCount() << 1) + 1);
		}
		String otherClassSpecName = null;
		for (int i = classFile.getMethodsCount() - 1; i >= 0; i--) {
			FieldMethodEntry method = classFile.getMethodAt(i);
			AttrCodeContent codeContent = method.findCode();
			String methodName = method.name().utfValue();
			boolean isDeleted = false;
			if (!keepBridgeMethods && method.accessFlags().clearVolatile()) {
				isChanged = true;
				if (codeContent != null
						&& isBridgeNoCastMethod(codeContent, methodName)) {
					classFile.removeMethodAt(i);
					isDeleted = true;
				}
			}
			if (!isDeleted) {
				if (fixSyntheticFieldMethod(method, classFile, false)) {
					isChanged = true;
				}
				if (method.accessFlags().clearTransient()) {
					isChanged = true;
				}
				if (codeContent != null) {
					if (methodName.equals("class$")
							&& method.accessFlags().isStatic()
							&& method.hasExceptionsSynthetic(false)
							&& method
									.descriptor()
									.utfValue()
									.equals("(Ljava/lang/String;)Ljava/lang/Class;")) {
						if (fixClassSpecMethod(codeContent, classFile)) {
							isChanged = true;
						}
						if (usedStaticFields == null
								&& classFile.getMethodsCount() - 1 == i) {
							usedStaticFields = new Hashtable();
						}
					}
					if (fixDualCasts(codeContent)) {
						isChanged = true;
					}
					int argSlots = method.getArgSlotsCount();
					if (argSlots > codeContent.maxLocals())
						throw new BadClassFileException();
					Hashtable catchesSet = new Hashtable(
							(codeContent.getCatchesCount() << 1) + 1);
					for (int j = 0, count = codeContent.getCatchesCount(); j < count; j++) {
						if (fixFinallyBlocks(codeContent, argSlots,
								codeContent.getCatch(j), catchesSet)) {
							int newCount = codeContent.getCatchesCount();
							if (count > newCount) {
								if ((j -= count - newCount + 1) < 0) {
									j = -1;
								}
							}
							count = newCount;
							isChanged = true;
						}
					}
					if (fixExcCatches(codeContent, method.isVoidRetType(),
							javaVer)) {
						isChanged = true;
					}
					if (superClassName != null) {
						if (methodName.equals("<init>")) {
							if (fixInnerInitMethod(codeContent, superClassName)) {
								isChanged = true;
							}
							if (isAnonymousClass
									&& fixInnerZeroInit(codeContent, classFile,
											superClassName)) {
								isChanged = true;
							}
						}
						if (assertDisabledFieldInd >= 0) {
							if (methodName.equals("<clinit>")
									&& !fixInnerSetAssertDisabled(codeContent,
											className)) {
								assertDisabledFieldInd = -1;
							}
							if (fixGetAssertionsDisabled(codeContent, className)) {
								isChanged = true;
							}
						}
					}
					String fieldClassName = fixOuterClassLiteral(codeContent,
							classFile);
					if (fieldClassName != null) {
						otherClassSpecName = fieldClassName;
					}
					if (fixLdcClassConst(codeContent, classFile)) {
						isChanged = true;
					}
					if (usedStaticFields != null) {
						collectUsedStaticFieldNames(usedStaticFields,
								codeContent, className);
					}
				}
			}
		}
		if (otherClassSpecName != null) {
			isChanged = true;
			int pos = otherClassSpecName.lastIndexOf('$') + 1;
			if (pos > 0 && otherClassSpecName.length() > pos
					&& otherClassSpecName.lastIndexOf('/') < pos - 2
					&& isAsciiDigit(otherClassSpecName.charAt(pos))) {
				classFile.removeStaticAnonInnerClassInfo(otherClassSpecName);
			}
		}
		if (assertDisabledFieldInd >= 0) {
			classFile.removeFieldAt(assertDisabledFieldInd);
			isChanged = true;
		}
		if (usedStaticFields != null
				&& removeUnusedClassSpecFields(classFile, usedStaticFields)) {
			isChanged = true;
		}
		boolean isFieldOfInnerClass = superClassName != null;
		for (int i = classFile.getFieldsCount() - 1; i >= 0; i--) {
			if (fixSyntheticFieldMethod(classFile.getFieldAt(i), classFile,
					isFieldOfInnerClass)) {
				isChanged = true;
			}
		}
		return isChanged;
	}

	private static boolean fixSyntheticFieldMethod(
			FieldMethodEntry fieldMethod, ClassFile classFile,
			boolean isFieldOfInnerClass) throws BadClassFileException {
		if (!fieldMethod.accessFlags().isSynthetic()
				|| fieldMethod.hasExceptionsSynthetic(false)
				|| (isFieldOfInnerClass && fieldMethod.accessFlags().isStatic() && fieldMethod
						.accessFlags().isFinal()))
			return false;
		fieldMethod.addAttribute(classFile.makeSyntheticAttribute());
		return true;
	}

	private static boolean isBridgeNoCastMethod(AttrCodeContent codeContent,
			String methodName) throws BadClassFileException {
		if (codeContent.getAstoreAloadVarAt(0, false) != 0)
			return false;
		int codeIndex = 1;
		while (codeContent.isXLoadAt(codeIndex)) {
			codeIndex++;
		}
		ConstantRef methodRef = codeContent.getInvokevirtualRefAt(codeIndex);
		return methodRef != null
				&& methodName.equals(methodRef.descriptor().classOrName()
						.utfValue())
				&& codeContent.isXReturnAt(codeContent.getOpCodesCount() - 1);
	}

	private static boolean fixClassSpecMethod(AttrCodeContent codeContent,
			ClassFile classFile) throws BadClassFileException {
		int codeIndex = codeContent.getOpCodesCount() - 1;
		ConstantRef methodRef = codeContent
				.getInvokevirtualRefAt(codeIndex - 1);
		if (methodRef == null || !codeContent.isAthrowAt(codeIndex))
			return false;
		ConstantRef fieldRef = codeContent.getConstRefAt(codeContent
				.findPutGetstatic(0));
		if (fieldRef != null
				&& fieldRef.descriptor().classOrName().utfValue().equals("cl$")) {
			codeContent.removeCodeAt(0, codeIndex + 1);
			setClassSpecMethodCode(codeContent, classFile);
		} else {
			ConstantRef methodDescr = methodRef.descriptor();
			if (!methodDescr.classOrName().utfValue().equals("initCause")
					|| !methodDescr
							.descriptor()
							.utfValue()
							.equals("(Ljava/lang/Throwable;)Ljava/lang/Throwable;"))
				return false;
			codeContent.insertCodeNopsAt(codeIndex, 1);
			ConstantRef methodClassRef = methodRef.classOrName();
			codeContent.putNewCheckcastAt(
					codeIndex,
					methodClassRef.classOrName().utfValue()
							.equals("java/lang/Throwable") ? classFile
							.addClassStringConst("java/lang/Error", true)
							: methodClassRef, false);
		}
		return true;
	}

	private static boolean fixDualCasts(AttrCodeContent codeContent) {
		int prevIndex = codeContent.findCheckcast(0);
		boolean isChanged = false;
		if (prevIndex >= 0) {
			do {
				int codeIndex = codeContent.findCheckcast(prevIndex + 1);
				if (codeIndex - 1 == prevIndex
						&& codeContent.getConstRefAt(codeIndex).isEqualTo(
								codeContent.getConstRefAt(prevIndex))) {
					codeContent.removeCodeAt(prevIndex, 1);
					isChanged = true;
				} else {
					if (codeIndex < 0)
						break;
					prevIndex = codeIndex;
				}
			} while (true);
		}
		return isChanged;
	}

	private static boolean fixFinallyBlocks(AttrCodeContent codeContent,
			int argSlots, ExceptionCatch excCatch, Hashtable catchesSet) {
		boolean isChanged = false;
		if (catchesSet.get(excCatch) == null) {
			int endIndex = excCatch.end().getIndex();
			if (excCatch.handler().getIndex() > endIndex
					&& excCatch.isAnyType()) {
				int startIndex = excCatch.start().getIndex();
				for (int j = 0, count = codeContent.getCatchesCount(); j < count; j++) {
					ExceptionCatch secondCatch = codeContent.getCatch(j);
					if (secondCatch.end().getIndex() <= startIndex
							&& secondCatch.handler().getIndex() >= endIndex
							&& fixFinallyBlocks(codeContent, argSlots,
									secondCatch, catchesSet)) {
						if (codeContent.getCatchesCount() != count)
							return true;
						startIndex = excCatch.start().getIndex();
						endIndex = excCatch.end().getIndex();
						isChanged = true;
					}
				}
				catchesSet.put(excCatch, "");
				if (fixFinallyBlocksFor(codeContent, argSlots, endIndex,
						excCatch.handler().getIndex())) {
					isChanged = true;
				}
			}
		}
		return isChanged;
	}

	private static boolean fixFinallyBlocksFor(AttrCodeContent codeContent,
			int argSlots, int endIndex, int handlerIndex) {
		int storedVarInd = codeContent.getAstoreAloadVarAt(handlerIndex, true);
		if (codeContent.isMonitorexitAt(handlerIndex
				+ (storedVarInd >= 0 ? 2 : 1))) {
			if ((handlerIndex - endIndex == 1 && !codeContent
					.isXReturnAt(endIndex))
					|| !codeContent.isMonitorexitAt(endIndex - 1)
					|| codeContent.hasBranchesOutsideIntoCode(endIndex - 1, 1))
				return false;
			int monitorVarInd;
			int jsrIndex;
			if (storedVarInd >= 0) {
				if (codeContent.getAstoreAloadVarAt(handlerIndex + 3, false) != storedVarInd)
					return false;
				monitorVarInd = codeContent.getAstoreAloadVarAt(
						handlerIndex + 1, false);
				jsrIndex = handlerIndex + 5;
			} else {
				monitorVarInd = codeContent.getAstoreAloadVarAt(handlerIndex,
						false);
				jsrIndex = handlerIndex + 3;
			}
			if (monitorVarInd < 0
					|| !codeContent.isAthrowAt(jsrIndex - 1)
					|| codeContent.getAstoreAloadVarAt(endIndex - 2, false) != monitorVarInd)
				return false;
			int retVarInd = codeContent.getAstoreAloadVarAt(jsrIndex, true);
			if (retVarInd < 0
					|| codeContent.getAstoreAloadVarAt(jsrIndex + 1, false) != monitorVarInd
					|| !codeContent.isMonitorexitAt(jsrIndex + 2)
					|| !codeContent.isRetAt(jsrIndex + 3, retVarInd)) {
				if (storedVarInd >= 0
						&& !codeContent.hasBranchesOutsideIntoCode(
								handlerIndex + 1, 3)) {
					if (codeContent.maxStack() < 2) {
						codeContent.setMaxStack(2);
					}
					codeContent.putAstoreAloadAt(handlerIndex, monitorVarInd,
							false);
					codeContent.putMonitorexitAt(handlerIndex + 1);
					codeContent.removeCodeAt(handlerIndex + 2, 2);
					jsrIndex = handlerIndex + 3;
				}
				retVarInd = codeContent.maxLocals();
				codeContent.setMaxLocals(retVarInd + 1);
				codeContent.insertCodeNopsAt(jsrIndex, 4);
				codeContent.putAstoreAloadAt(jsrIndex, retVarInd, true);
				codeContent
						.putAstoreAloadAt(jsrIndex + 1, monitorVarInd, false);
				codeContent.putMonitorexitAt(jsrIndex + 2);
				codeContent.putRetAt(jsrIndex + 3, retVarInd);
			}
			codeContent.removeCodeAt(endIndex - 1, 1);
			codeContent.putJsrGotoAt(endIndex - 2, jsrIndex - 1, true);
			return true;
		} else {
			int[] indexRef = new int[1];
			boolean isChanged = false;
			do {
				int blockLen;
				indexRef[0] = -1;
				int jsrIndex = codeContent.getJsrGotoTargetIndexAt(
						handlerIndex + 1, true);
				if (handlerIndex + 4 != jsrIndex) {
					int aloadIndex = codeContent.findRetAload(handlerIndex + 1,
							storedVarInd, false);
					blockLen = aloadIndex - handlerIndex - 1;
					if (!codeContent.isAthrowAt(aloadIndex + 1)
							|| !codeContent.isSameCodeRegions(endIndex,
									handlerIndex + 1, aloadIndex, argSlots,
									indexRef)
							|| codeContent.hasBranchesOutsideIntoCode(
									endIndex + 1, blockLen - 1)
							|| codeContent.hasBranchesOutsideIntoCode(
									handlerIndex + 1, blockLen + 2))
						break;
					int retVarInd = codeContent.maxLocals();
					codeContent.setMaxLocals(retVarInd + 1);
					codeContent.putRetAt(aloadIndex, retVarInd);
					codeContent.removeCodeAt(aloadIndex + 1, 1);
					codeContent.insertCodeNopsAt(handlerIndex + 1, 4);
					jsrIndex = handlerIndex + 4;
					codeContent.putJsrGotoAt(handlerIndex + 1, jsrIndex, true);
					codeContent.putAstoreAloadAt(handlerIndex + 2,
							storedVarInd, false);
					codeContent.putAthrowAt(handlerIndex + 3);
					codeContent.putAstoreAloadAt(jsrIndex, retVarInd, true);
					if (indexRef[0] > handlerIndex) {
						indexRef[0] += 3;
					}
				} else {
					if (storedVarInd < 0
							|| codeContent.getAstoreAloadVarAt(jsrIndex - 2,
									false) != storedVarInd
							|| !codeContent.isAthrowAt(jsrIndex - 1))
						break;
					int retIndex = codeContent.findRetAload(jsrIndex + 1,
							codeContent.getAstoreAloadVarAt(jsrIndex, true),
							true);
					blockLen = retIndex - jsrIndex - 1;
					if (blockLen != 0 ? !codeContent.isSameCodeRegions(
							endIndex, jsrIndex + 1, retIndex, argSlots,
							indexRef)
							|| codeContent.hasBranchesOutsideIntoCode(
									endIndex + 1, blockLen - 1)
							: codeContent.getJsrGotoTargetIndexAt(endIndex,
									false) < 0
									&& !codeContent
											.isXReturnAt(endIndex
													+ (codeContent
															.isXLoadAt(endIndex) ? 1
															: 0)))
						break;
				}
				codeContent.removeCodeAt(endIndex + 1, blockLen - 1);
				codeContent.putJsrGotoAt(endIndex, jsrIndex - blockLen + 1,
						true);
				isChanged = true;
				endIndex++;
				if (indexRef[0] >= 0) {
					codeContent.insertCodeNopsAt(endIndex, 1);
					if (indexRef[0] >= endIndex) {
						indexRef[0] -= blockLen - 2;
					}
					codeContent.putJsrGotoAt(endIndex, indexRef[0], false);
					handlerIndex++;
				}
				do {
					if (codeContent.getJsrGotoTargetIndexAt(endIndex, true) <= endIndex) {
						if (!codeContent.isMonitorexitAt(endIndex + 1)
								|| codeContent.getAstoreAloadVarAt(endIndex,
										false) < 0)
							break;
						endIndex++;
					}
					endIndex++;
				} while (true);
				if (codeContent.getJsrGotoTargetIndexAt(endIndex, false) < 0) {
					if (codeContent.isXLoadAt(endIndex)) {
						endIndex++;
					}
					if (!codeContent.isXReturnAt(endIndex))
						break;
				}
				endIndex++;
				handlerIndex -= blockLen - 1;
			} while (endIndex < handlerIndex);
			return isChanged;
		}
	}

	private static boolean fixExcCatches(AttrCodeContent codeContent,
			boolean isVoidRetType, int javaVer) {
		boolean isChanged = false;
		for (int i = codeContent.getCatchesCount() - 1; i >= 0; i--) {
			ExceptionCatch excCatch = codeContent.getCatch(i);
			CodeAbsLabel endLabel = excCatch.end();
			int endIndex = endLabel.getIndex();
			int handlerIndex = excCatch.handler().getIndex();
			if (endIndex < handlerIndex) {
				int newEndIndex = endIndex;
				boolean changeEnd = false;
				if (excCatch.isAnyType()) {
					newEndIndex = handlerIndex;
					int blockLen = handlerIndex - endIndex;
					if ((blockLen == 2 ? codeContent.getJsrGotoTargetIndexAt(
							endIndex, true) <= handlerIndex
							: (blockLen != 1 && blockLen != 3)
									|| codeContent.getAstoreAloadVarAt(
											handlerIndex - 3, false) < 0
									|| !codeContent
											.isMonitorexitAt(handlerIndex - 2))
							|| (!codeContent.isXReturnAt(handlerIndex - 1) && codeContent
									.getJsrGotoTargetIndexAt(handlerIndex - 1,
											false) < 0)) {
						changeEnd = true;
					}
				} else {
					boolean isNext = false;
					do {
						int codeIndex = newEndIndex;
						while (codeContent.getJsrGotoTargetIndexAt(codeIndex,
								true) > handlerIndex) {
							codeIndex++;
						}
						if (codeIndex - newEndIndex > 1) {
							isNext = true;
						}
						if (codeContent.getJsrGotoTargetIndexAt(codeIndex,
								false) < 0) {
							if (codeContent.isXLoadAt(codeIndex)) {
								codeIndex++;
							}
							if (codeContent.isXReturnAt(codeIndex)) {
								if (codeIndex != newEndIndex
										|| (javaVer <= 3 && !isVoidRetType)) {
									isNext = true;
								}
							} else {
								if (!codeContent.isMonitorexitAt(codeIndex)
										|| codeContent.getAstoreAloadVarAt(
												codeIndex - 1, false) < 0)
									break;
								isNext = true;
							}
						}
						changeEnd = isNext;
						newEndIndex = codeIndex + 1;
						isNext = true;
					} while (true);
				}
				if (newEndIndex != endIndex) {
					boolean isMerged = false;
					for (int j = codeContent.getCatchesCount() - 1; j >= 0; j--) {
						if (j != i) {
							ExceptionCatch secondCatch = codeContent
									.getCatch(j);
							int secondStart = secondCatch.start().getIndex();
							if (endIndex < secondStart
									&& secondStart <= newEndIndex
									&& secondCatch.handler().getIndex() == handlerIndex
									&& secondStart < handlerIndex
									&& secondCatch.isSameCatchType(excCatch)) {
								int secondEnd = secondCatch.end().getIndex();
								codeContent.removeCatch(j);
								isChanged = true;
								if (j < i) {
									i--;
								}
								if (newEndIndex <= secondEnd) {
									newEndIndex = secondEnd;
									isMerged = true;
								}
							}
						}
					}
					if (isMerged) {
						changeEnd = true;
						i++;
					}
					if (changeEnd) {
						endLabel.setNewIndex(newEndIndex);
						isChanged = true;
					}
				}
			}
		}
		for (int i = codeContent.getCatchesCount() - 1; i >= 0; i--) {
			ExceptionCatch excCatch = codeContent.getCatch(i);
			CodeAbsLabel endLabel = excCatch.end();
			int handlerIndex = excCatch.handler().getIndex();
			if (endLabel.getIndex() > handlerIndex) {
				int startIndex = excCatch.start().getIndex();
				if (startIndex < handlerIndex) {
					endLabel.setNewIndex(handlerIndex);
					isChanged = true;
				} else if (startIndex == handlerIndex) {
					codeContent.removeCatch(i);
					isChanged = true;
				}
			}
		}
		return isChanged;
	}

	private static boolean fixInnerInitMethod(AttrCodeContent codeContent,
			String superClassName) throws BadClassFileException {
		int blockLen = 0;
		while (codeContent.getAstoreAloadVarAt(blockLen, false) == 0
				&& codeContent.isXLoadAt(blockLen + 1)) {
			ConstantRef fieldRef = codeContent.getPutfieldRefAt(blockLen + 2);
			if (fieldRef == null)
				break;
			String fieldName = fieldRef.descriptor().classOrName().utfValue();
			if (!fieldName.startsWith("this$") && !fieldName.startsWith("val$"))
				break;
			blockLen += 3;
		}
		if (blockLen == 0
				|| codeContent.hasBranchesOutsideIntoCode(1, blockLen - 1))
			return false;
		int invokeIndex = blockLen;
		do {
			invokeIndex = codeContent.findInvokestaticSpecial(invokeIndex,
					false);
			if (invokeIndex < 0)
				return false;
			ConstantRef methodRef = codeContent.getConstRefAt(invokeIndex);
			if (methodRef.descriptor().classOrName().utfValue()
					.equals("<init>")
					&& methodRef.getEntityClassNameValue().equals(
							superClassName))
				break;
			invokeIndex++;
		} while (true);
		codeContent.moveCodeBlockAt(0, blockLen, invokeIndex + 1);
		return true;
	}

	private static boolean fixInnerZeroInit(AttrCodeContent codeContent,
			ClassFile classFile, String superClassName)
			throws BadClassFileException {
		if (codeContent.getAstoreAloadVarAt(0, false) != 0)
			return false;
		int startIndex = codeContent.findInvokestaticSpecial(1, false);
		if (startIndex < 0)
			return false;
		ConstantRef methodRef = codeContent.getConstRefAt(startIndex);
		if (!methodRef.descriptor().classOrName().utfValue().equals("<init>")
				|| !methodRef.getEntityClassNameValue().equals(superClassName))
			return false;
		for (int codeIndex = startIndex - 1; codeIndex > 0; codeIndex--) {
			if (!codeContent.isXLoadAt(codeIndex))
				return false;
		}
		int codeLimit = codeContent.getOpCodesCount();
		for (int codeIndex = startIndex + 1; codeIndex < codeLimit; codeIndex++) {
			if (codeContent.isTargetInRangeAt(codeIndex, 0, codeIndex - 1))
				return false;
		}
		Hashtable names = new Hashtable();
		boolean isChanged = false;
		String className = classFile.className();
		while (++startIndex < codeLimit
				&& !codeContent.isInvokeMethodAt(startIndex)) {
			ConstantRef fieldRef = codeContent.getPutfieldRefAt(startIndex);
			if (fieldRef != null) {
				ConstantRef fieldDescr = fieldRef.descriptor();
				String fieldName = fieldDescr.classOrName().utfValue();
				if (names.put(fieldName, "") == null
						&& codeContent.getAstoreAloadVarAt(startIndex - 2,
								false) == 0
						&& codeContent.isXConstZeroAt(startIndex - 1)
						&& className.equals(fieldRef.getEntityClassNameValue())) {
					int fieldInd = classFile.findField(fieldName, fieldDescr
							.descriptor().utfValue(), false);
					if (fieldInd >= 0
							&& !classFile.getFieldAt(fieldInd).accessFlags()
									.isFinal()
							&& !codeContent.hasBranchesOutsideIntoCode(
									startIndex - 1, 2)) {
						startIndex -= 3;
						codeLimit -= 3;
						codeContent.removeCodeAt(startIndex + 1, 3);
						isChanged = true;
					}
				}
			}
		}
		return isChanged;
	}

	private static boolean fixInnerSetAssertDisabled(
			AttrCodeContent codeContent, String className)
			throws BadClassFileException {
		int codeIndex = 5;
		while ((codeIndex = codeContent.findPutGetstatic(codeIndex + 1)) >= 0) {
			if (isAssertionsDisabledField(
					codeContent.getPutGetstaticRefAt(codeIndex, true),
					className))
				break;
		}
		if (codeIndex < 0)
			return true;
		int startIndex = codeIndex - 6;
		if (codeContent.findLdcClass(startIndex, startIndex + 1) < 0) {
			startIndex -= 7;
			ConstantRef fieldRef = codeContent.getPutGetstaticRefAt(startIndex,
					false);
			if (fieldRef == null)
				return false;
			ConstantRef fieldDescr = fieldRef.descriptor();
			if (!fieldDescr.classOrName().utfValue().startsWith("class$")
					|| !fieldDescr.descriptor().utfValue()
							.equals("Ljava/lang/Class;")
					|| !fieldRef.isEqualTo(codeContent
							.getConstRefAt(startIndex + 7)))
				return false;
		}
		ConstantRef methodRef = codeContent
				.getInvokevirtualRefAt(codeIndex - 5);
		if (methodRef == null)
			return false;
		ConstantRef methodDescr = methodRef.descriptor();
		if (!methodDescr.descriptor().utfValue().equals("()Z")
				|| codeContent.getJsrGotoTargetIndexAt(codeIndex - 2, false) != codeIndex
				|| codeContent.hasBranchesOutsideIntoCode(startIndex + 1,
						codeIndex - startIndex))
			return false;
		codeContent.removeCodeAt(startIndex, codeIndex - startIndex + 1);
		return true;
	}

	private static boolean isAssertionsDisabledField(ConstantRef fieldRef,
			String className) throws BadClassFileException {
		return fieldRef != null
				&& fieldRef.descriptor().classOrName().utfValue()
						.equals("$assertionsDisabled")
				&& className.equals(fieldRef.getEntityClassNameValue());
	}

	private static boolean fixGetAssertionsDisabled(
			AttrCodeContent codeContent, String className)
			throws BadClassFileException {
		int codeIndex = -1;
		boolean isChanged = false;
		while ((codeIndex = codeContent.findPutGetstatic(codeIndex + 1)) >= 0) {
			if (isAssertionsDisabledField(
					codeContent.getPutGetstaticRefAt(codeIndex, false),
					className)) {
				codeContent.putIconstZeroAt(codeIndex);
				isChanged = true;
			}
		}
		return isChanged;
	}

	private static String fixOuterClassLiteral(AttrCodeContent codeContent,
			ClassFile classFile) throws BadClassFileException {
		String className = classFile.className();
		int codeIndex = findInvokeOuterClassSpec(codeContent, 0, className);
		String fieldClassName = null;
		if (codeIndex >= 0) {
			ConstantRef classMethod = addClassSpecMethod(classFile);
			if (classMethod != null) {
				do {
					codeContent.putInvokestaticAt(codeIndex, classMethod);
					ConstantRef fieldRef = codeContent.getPutGetstaticRefAt(
							codeIndex + 2, true);
					if (fieldRef != null) {
						ConstantRef fieldDescr = fieldRef.descriptor();
						String fieldName = fieldDescr.classOrName().utfValue();
						if ((fieldName.startsWith("class$") || fieldName
								.startsWith("array$"))
								&& fieldDescr.descriptor().utfValue()
										.equals("Ljava/lang/Class;")) {
							ConstantRef classFieldRef = classFile
									.addStaticField(fieldName,
											"Ljava/lang/Class;");
							if (classFieldRef != null) {
								fieldClassName = fieldRef
										.getEntityClassNameValue();
								codeContent.putPutGetstaticAt(codeIndex + 2,
										classFieldRef, true);
								if (fieldRef.isEqualTo(codeContent
										.getPutGetstaticRefAt(codeIndex - 3,
												false))) {
									codeContent
											.putPutGetstaticAt(codeIndex - 3,
													classFieldRef, false);
									if (fieldRef.isEqualTo(codeContent
											.getPutGetstaticRefAt(
													codeIndex + 4, false))) {
										codeContent.putPutGetstaticAt(
												codeIndex + 4, classFieldRef,
												false);
									}
								} else if (fieldRef.isEqualTo(codeContent
										.getPutGetstaticRefAt(codeIndex - 5,
												false))) {
									codeContent
											.putPutGetstaticAt(codeIndex - 5,
													classFieldRef, false);
								}
							}
						}
					}
					codeIndex = findInvokeOuterClassSpec(codeContent,
							codeIndex + 1, className);
				} while (codeIndex >= 0);
			}
		}
		return fieldClassName;
	}

	private static int findInvokeOuterClassSpec(AttrCodeContent codeContent,
			int codeIndex, String className) throws BadClassFileException {
		while ((codeIndex = codeContent
				.findInvokestaticSpecial(codeIndex, true)) >= 0) {
			ConstantRef methodRef = codeContent.getConstRefAt(codeIndex);
			ConstantRef methodDescr = methodRef.descriptor();
			if (methodDescr.classOrName().utfValue().equals("class$")
					&& methodDescr.descriptor().utfValue()
							.equals("(Ljava/lang/String;)Ljava/lang/Class;")
					&& !className.equals(methodRef.getEntityClassNameValue()))
				break;
			codeIndex++;
		}
		return codeIndex;
	}

	private static ConstantRef addClassSpecMethod(ClassFile classFile)
			throws BadClassFileException {
		int methodIndex = classFile.addStaticMethodNoExc("class$",
				"(Ljava/lang/String;)Ljava/lang/Class;");
		if (methodIndex < 0)
			return null;
		AttrCodeContent codeContent = classFile.getMethodAt(methodIndex)
				.findCode();
		if (codeContent == null)
			return null;
		if (codeContent.getOpCodesCount() == 0) {
			setClassSpecMethodCode(codeContent, classFile);
		}
		return classFile.addNormMethodConstFor(methodIndex);
	}

	private static void setClassSpecMethodCode(AttrCodeContent codeContent,
			ClassFile classFile) {
		codeContent.setMaxStack(3);
		codeContent.setMaxLocals(2);
		codeContent.insertCodeNopsAt(0, 10);
		codeContent.putAstoreAloadAt(0, 0, false);
		codeContent.putInvokestaticAt(1, classFile.addNormMethodConst(
				classFile.addClassStringConst("java/lang/Class", true),
				"forName", "(Ljava/lang/String;)Ljava/lang/Class;"));
		codeContent.putAreturnAt(2);
		codeContent.putAstoreAloadAt(3, 1, true);
		ConstantRef noClassDefErrConst = classFile.addClassStringConst(
				"java/lang/NoClassDefFoundError", true);
		codeContent.putNewCheckcastAt(4, noClassDefErrConst, true);
		codeContent.putDupAt(5);
		codeContent.putAstoreAloadAt(6, 1, false);
		ConstantRef classNotFoundExcConst = classFile.addClassStringConst(
				"java/lang/ClassNotFoundException", true);
		codeContent.putInvokevirtualSpecialAt(7, classNotFoundExcConst,
				"getMessage", "()Ljava/lang/String;", true, classFile);
		codeContent.putInvokevirtualSpecialAt(8, noClassDefErrConst, "<init>",
				"(Ljava/lang/String;)V", false, classFile);
		codeContent.putAthrowAt(9);
		codeContent.addCatch(0, 3, 3, classNotFoundExcConst);
	}

	private static boolean fixLdcClassConst(AttrCodeContent codeContent,
			ClassFile classFile) throws BadClassFileException {
		int codeIndex = codeContent.findLdcClass(0,
				codeContent.getOpCodesCount());
		if (codeIndex < 0)
			return false;
		ConstantRef classMethod = addClassSpecMethod(classFile);
		boolean isChanged = false;
		if (classMethod != null) {
			do {
				String classConstStr = codeContent.getConstRefAt(codeIndex)
						.classOrName().utfValue().replace('/', '.');
				ConstantRef classFieldRef = classFile
						.addStaticField(
								(classConstStr.startsWith("[") ? "array"
										: "class$")
										+ (classConstStr.endsWith(";") ? classConstStr.substring(
												0, classConstStr.length() - 1)
												: classConstStr).replace('.',
												'$').replace('[', '$'),
								"Ljava/lang/Class;");
				if (classFieldRef != null) {
					if (!isChanged) {
						codeContent.setMaxStack(codeContent.maxStack() + 1);
						isChanged = true;
					}
					codeContent.putPutGetstaticAt(codeIndex, classFieldRef,
							false);
					codeContent.insertCodeNopsAt(codeIndex + 1, 7);
					codeContent.putIfnonnullAt(codeIndex + 1, codeIndex + 7);
					codeContent.putLdcClassStringAt(codeIndex + 2,
							classConstStr, false, classFile);
					codeContent.putInvokestaticAt(codeIndex + 3, classMethod);
					codeContent.putDupAt(codeIndex + 4);
					codeContent.putPutGetstaticAt(codeIndex + 5, classFieldRef,
							true);
					codeContent.putJsrGotoAt(codeIndex + 6, codeIndex + 8,
							false);
					codeIndex += 7;
					codeContent.putPutGetstaticAt(codeIndex, classFieldRef,
							false);
				}
				codeIndex = codeContent.findLdcClass(codeIndex + 1,
						codeContent.getOpCodesCount());
			} while (codeIndex >= 0);
		}
		return isChanged;
	}

	private static void collectUsedStaticFieldNames(Hashtable usedStaticFields,
			AttrCodeContent codeContent, String className)
			throws BadClassFileException {
		int codeIndex = -1;
		while ((codeIndex = codeContent.findPutGetstatic(codeIndex + 1)) >= 0) {
			ConstantRef fieldRef = codeContent.getConstRefAt(codeIndex);
			if (className.equals(fieldRef.getEntityClassNameValue())) {
				usedStaticFields.put(fieldRef.descriptor().classOrName()
						.utfValue(), "");
			}
		}
	}

	private static boolean removeUnusedClassSpecFields(ClassFile classFile,
			Hashtable usedStaticFields) throws BadClassFileException {
		boolean isChanged = false;
		for (int i = classFile.getFieldsCount() - 1; i >= 0; i--) {
			FieldMethodEntry field = classFile.getFieldAt(i);
			if (field.accessFlags().isStatic()) {
				String fieldName = field.name().utfValue();
				if (usedStaticFields.get(fieldName) == null
						&& field.hasExceptionsSynthetic(false)
						&& (fieldName.equals("cl$") ? field.descriptor()
								.utfValue().equals("Ljava/lang/ClassLoader;")
								: (fieldName.startsWith("class$") || fieldName
										.startsWith("array$"))
										&& field.descriptor().utfValue()
												.equals("Ljava/lang/Class;"))) {
					classFile.removeFieldAt(i);
					isChanged = true;
				}
			}
		}
		return isChanged;
	}
}
