/*
 * Copyright 2024 Creative Scala
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package introduction

import scala.concurrent.duration.*
import cats.syntax.all.*

import cats.effect.*

// The goal of these exercises is to introduce some of the basics of using IO:
//
// - constructing IO
// - doing one thing after another (sequencing)
// - combining multiple independent IOs
//
// and additionally get to the fundamental design principle of IO: the
// separation between descriptions and action
object Basics extends IOApp.Simple {
  // 0. This object extends IOApp.Simple. This allows us to run the object. It
  // expects an IO with the name `run` which describes the program it will run.
  // So replace `run` below with whatever you want to see when your program is
  // run.
  // val run = IO(println("Hello!"))

  // 1. You can create an IO using IO(...) and IO.pure(...). (The former is the
  // apply method on the IO companion object, so can also be written as
  // IO.apply(...))
  //
  // What is the difference between these methods? Can you write a program that
  // demonstrates the difference?

  // IO.pure doesn't defer the computation of the value until end of world, it runs it at the place of description itself.
  // IO.pure should only be used if the value has already been computed.
  // whereas IO.apply suspends a synchronous side effect in IO.

  //  val program: IO[Unit] = {
  //    val pure: IO[Unit] = IO.pure(println("Before Sleep"))
  //    val apply: IO[Unit] = IO.apply(println("After Sleep"))
  //
  //    IO.sleep(1.second).flatMap(_ => pure).flatMap(_ => apply)
  //  }
  // val run: IO[Unit] = program

  // 2. flatMap allows us to do one thing after another, where the thing we do
  // second depends on the result of the thing we do first.
  //
  // IO.realTime gives the time since the start of the epoch. Using flatMap
  // write a program that gets the time and prints it on the console.

  //  val program2: IO[Unit] = {
  //    IO.realTime.flatMap(IO.println)
  //  }
  //  val run: IO[Unit] = program2

  // 3. mapN allows us to combine two or more IOs that don't depend on each
  // other. Here's an example:
  //
  // (IO(1), IO(2)).mapN((x, y) => x + y)
  //
  // When this program is run, are the IOs always evaluated in a particular
  // order (e.g. left to right) or is it non-deterministic?

  // the IOs are always evaluated in a particular order.
  // It's always the same: left-to-right

  //  val program3: IO[Unit] = {
  //    val ioa: IO[Int] = IO.println("IO A").as(1)
  //    val iob: IO[Int] = IO.println("IO B").as(2)
  //
  //    (ioa, iob).mapN(_ + _)
  //  }
  //  val run: IO[Unit] = program3

  // 4. The following program attempts to add logging before and after an IO
  // runs. Does it do this correctly?
  //  def log[A](io: IO[A]): Unit = {
  //    println("Starting the IO")
  //    val _ = io
  //    println("Ending the IO")
  //  }

  // The above log method is incorrect!
  // IO is a description, so writing val _ = io does nothing.

  // 5. Write a method that adds logging before and after an IO
  //  def log[A](io: IO[A]): IO[A] = {
  //    IO.println("Starting the IO")
  //      .flatMap(_ => io)
  //      .flatMap{ a =>
  //        IO.println("Ending the IO").map(_ => a)
  //      }
  //  }
  // Here's an example of how to use it:
  //
  //  val program5 = log(IO.realTime).flatMap(time => IO.println(s"The time is $time"))
  //  val run: IO[Unit] = program5

  // 6. What do the *> and <* methods do? Could you use them in the logging
  // method you just wrote?
  def log[A](io: IO[A]): IO[A] =
    IO.println("Starting the IO") *> io <* IO.println("Ending the IO")

  val program6 =
    log(IO.realTime).flatMap(time => IO.println(s"The time is $time"))
  val run: IO[Unit] = program6
}
