#!/usr/bin/env python3
"""Build the local plugin using this machine's IDE and cached Kotlin compiler."""

import argparse
import hashlib
import re
import subprocess
import tempfile
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, required=True, help="Source directory or source ZIP")
    parser.add_argument("--ide", type=Path, default=Path("/snap/clion/current"))
    parser.add_argument("--version", default="1.3.2-local263.2")
    parser.add_argument("--output", type=Path, default=Path(__file__).resolve().parent)
    args = parser.parse_args()
    cache = Path.home() / ".gradle/caches/modules-2/files-2.1"
    dependencies = [
        ("org.jetbrains.kotlin", "kotlin-compiler-embeddable", "2.0.21"),
        ("org.jetbrains.kotlin", "kotlin-stdlib", "2.0.21"),
        ("org.jetbrains.kotlin", "kotlin-script-runtime", "2.0.21"),
        ("org.jetbrains.kotlin", "kotlin-reflect", "1.6.10"),
        ("org.jetbrains.intellij.deps", "trove4j", "1.0.20200330"),
        ("org.jetbrains.kotlinx", "kotlinx-coroutines-core-jvm", "1.9.0"),
        ("org.jetbrains", "annotations", "23.0.0"),
    ]
    compiler = []
    for group, name, version in dependencies:
        jars = list((cache / group / name / version).rglob("*.jar"))
        if not jars:
            raise SystemExit(f"Missing cached compiler dependency: {group}:{name}:{version}")
        compiler.append(jars[0])
    libs = list((args.ide / "lib").glob("*.jar"))
    for plugin in ("nativeDebug-plugin", "clion"):
        libs.extend((args.ide / "plugins" / plugin / "lib").rglob("*.jar"))
    build = (args.ide / "build.txt").read_text().strip().removeprefix("CL-")
    args.output.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="waveform-build-") as temporary:
        work = Path(temporary)
        if args.source.is_file():
            unpacked = work / "source"
            with ZipFile(args.source) as archive:
                for entry in archive.namelist():
                    if not (unpacked / entry).resolve().is_relative_to(unpacked.resolve()):
                        raise SystemExit("Source ZIP contains an unsafe path")
                archive.extractall(unpacked)
            resources_file = next(unpacked.rglob("src/main/resources/META-INF/plugin.xml"))
            source = resources_file.parents[4]
        else:
            source = args.source.resolve()
        classes = work / "classes"
        classes.mkdir()
        command = [
            "java", "-Xmx2g", "-cp", ":".join(map(str, compiler)),
            "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler", "-no-stdlib", "-no-reflect",
            "-Xskip-metadata-version-check", "-jvm-target", "17", "-classpath",
            ":".join(map(str, libs)), "-d", str(classes),
        ]
        command.extend(map(str, sorted((source / "src/main/kotlin").rglob("*.kt"))))
        subprocess.run(command, check=True, timeout=120)
        resources = source / "src/main/resources"
        xml = (resources / "META-INF/plugin.xml").read_text()
        xml = re.sub(r"<version>[^<]*</version>\s*", "", xml)
        xml = re.sub(r"<idea-version\b[^>]*/>\s*", "", xml)
        xml = xml.replace("<idea-plugin>", (
            f"<idea-plugin>\n    <version>{args.version}</version>\n"
            f'    <idea-version since-build="{build}" until-build="{build.split(".")[0]}.*"/>'
        ), 1)
        jar = work / f"clion-waveform-plotter-{args.version}.jar"
        with ZipFile(jar, "w", ZIP_DEFLATED) as archive:
            for path in sorted(classes.rglob("*")):
                if path.is_file():
                    archive.write(path, path.relative_to(classes).as_posix())
            for path in sorted(resources.rglob("*")):
                if path.is_file() and path != resources / "META-INF/plugin.xml":
                    archive.write(path, path.relative_to(resources).as_posix())
            archive.writestr("META-INF/plugin.xml", xml)
            archive.writestr("META-INF/MANIFEST.MF", f"Manifest-Version: 1.0\nImplementation-Version: {args.version}\n\n")
        target = args.output / f"clion-waveform-plotter-{args.version}.zip"
        with ZipFile(target, "w", ZIP_DEFLATED) as archive:
            archive.write(jar, "clion-waveform-plotter/lib/" + jar.name)
            archive.write(source / "README.md", "clion-waveform-plotter/README.upstream.md")
        print(f"Built against CL-{build}: {target}")
        print("SHA256:", hashlib.sha256(target.read_bytes()).hexdigest())


if __name__ == "__main__":
    main()
