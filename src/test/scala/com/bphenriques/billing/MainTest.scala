package com.bphenriques.billing

import cats.effect.ExitCode
import fs2.io.file.Path
import munit.CatsEffectSuite

class MainTest extends CatsEffectSuite {
  private val validSamplesFolder: Path = Path(getClass.getClassLoader.getResource("valid-samples").getPath)
  private val invalidSamplesFolder: Path = Path(getClass.getClassLoader.getResource("invalid-samples").getPath)

  test("It returns success exit code if successful") {
    Main.run(List((validSamplesFolder / "sample.csv").absolute.toString)).assertEquals(ExitCode.Success)
  }

  test("It returns error exit code if fails") {
    Main.run(List((invalidSamplesFolder / "bad-time-format.csv").absolute.toString)).assertEquals(ExitCode.Error)
  }
}
