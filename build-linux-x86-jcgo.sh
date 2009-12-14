#!/bin/sh
# @(#) build-linux-x86-jcgo.sh - Linux/x86 build script for JadRetro.
# Used tools: JCGO, GNU/GCC.

proj_unix_name="jadretro"
dist_dir=".dist-linux-x86-jcgo"

echo "Building Linux/x86 executable using JCGO+GCC..."

if [ "$jcgo_home" != "" ] ; then : ; else : ; jcgo_home="/usr/share/JCGO" ; fi

rm -rf "$dist_dir"
mkdir "$dist_dir"
mkdir "$dist_dir/.jcgo_Out"

$jcgo_home/jcgo -d "$dist_dir/.jcgo_Out" -src $~/goclsp/clsp_asc -src src net.sf.$proj_unix_name.Main @$~/stdpaths.in || exit 1

mkdir "$dist_dir/$proj_unix_name"
gcc -o "$dist_dir/$proj_unix_name/$proj_unix_name" -I $jcgo_home/include -I $jcgo_home/include/boehmgc -I $jcgo_home/native -Os -fwrapv -fno-strict-aliasing -freorder-blocks -DJCGO_INTFIT -DJCGO_UNIX -DJCGO_UNIFSYS -DJCGO_USEGCJ -DJCGO_NOJNI -DJCGO_NOSEGV -DEXTRASTATIC=static -DJNIIMPORT=static/**/inline -DJNIEXPORT=JNIIMPORT -DJNUBIGEXPORT=static -DGCSTATICDATA= -DJCGO_GCRESETDLS -DJCGO_NOFP -s "$dist_dir/.jcgo_Out/Main.c" $jcgo_home/libs/x86/linux/libgc.a || exit 1

cp -p GNU_GPL.txt $proj_unix_name.txt "$dist_dir/$proj_unix_name"
echo ""

"$dist_dir/$proj_unix_name/$proj_unix_name"

echo ""
echo "BUILD SUCCESSFUL"
exit 0
