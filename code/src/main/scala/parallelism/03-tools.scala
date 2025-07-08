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

package parallelism

import cats.effect.*
import cats.effect.std.Semaphore
import cats.syntax.all.*

import scala.concurrent.duration.*
import scala.util.Random

// The goal is to understand how to work with the concurrent tools in the Cats
// Effects standard library.
//
// We can get very far with just `parMapN` and friends, but here we'll see some
// uses the go beyond what we can achieve with just returning values.
object Tools extends IOApp.Simple {

  // ---------------------------------------------------------------------------
  // Challenge One
  // ---------------------------------------------------------------------------
  //
  // The first challenge is about creating refs.
  val refIO = IO.ref(0)

  def printAndIncrement(name: String)(ref: Ref[IO, Int]): IO[Int] =
    for {
      v <- ref.updateAndGet(x => x + 1)
      _ <- IO.println(s"$name: Ref's value was $v")
    } yield v

  val first =
    (
      refIO.flatMap(printAndIncrement("Left")),
      refIO.flatMap(printAndIncrement("Right"))
    )
      .parMapN((l, r) => s"Values were $l and $r")
      .flatMap(IO.println)

  val second =
    refIO.flatMap { r =>
      (printAndIncrement("Left")(r), printAndIncrement("Right")(r))
        .parMapN((l, r) => s"Values were $l and $r")
        .flatMap(IO.println)
    }

  // first and second are two programs that attempt to use a shared `Ref`. What
  // behaviour do you observe when these two programs are run. Why do you see
  // that behaviour? Which one do you think would be correct if we wanted to use
  // a `Ref` to coordinate two concurrent processes?
  // val run = IO.println("First") *> first *> IO.println("Second") *> second

  // ---------------------------------------------------------------------------
  // Challenge Two
  // ---------------------------------------------------------------------------
  //
  // In many applications it's better to tradeoff quality of results for faster
  // results. Search is a good example. The user usually doesn't care about
  // finding the best resource for their query if they have to wait a long time,
  // but rather quickly finding a good enough source that solves their problem.
  //
  // In this challenge we'll simulate this situation with a `Ref` that
  // accumulates a result, and another process that takes the result after a
  // certain time. For simplicity, our result will just be a number stored in
  // the `Ref`. You can reuse `ref` above for this.

  val random: Random.type = scala.util.Random
  val smallRandomSleep: IO[Unit] =
    IO(random.nextInt(100)).flatMap(ms => IO.sleep(ms.millis))

  // This is the process that generates a result. Calling `replicateA_` repeats
  // it 100 times. You need to modify this so it adds the values it generates to
  // the shared ref. Then run five of these processes in parallel.
//  def generate(ref: Ref[IO, Int]): IO[Unit] = {
//    val process: IO[Unit] =
//      smallRandomSleep
//        .map(_ => random.nextInt(10))
//        .flatMap(i => ref.update(_ + i))
//        .replicateA_(100)
//    List.fill(5)(process).parSequence.void
//  }

  // This is the process that collects the result. Modify it so it gets the
  // value from the shared ref when the sleep finishes, and then prints out that
  // value. It should run in parallel with the generators.
//  def collector(ref: Ref[IO, Int]): IO[Unit] =
//    IO.sleep(1.second) *> ref.get.flatMap(IO.println)

//  val run: IO[Unit] = {
//    refIO.flatMap { ref =>
//      (generate(ref), collector(ref)).parTupled
//    }.void
//  }

  // ---------------------------------------------------------------------------
  // Challenge Three
  // ---------------------------------------------------------------------------
  //
  // This is a variation of the above challenge. Instead of waiting a fixed
  // amount of time and then collecting the available results, we wait for a
  // fixed number of results to become available. For this challenge we'll use a
  // Semaphore in addition to a Ref.
  //
  // Use the same setup as the previous challenge, but in this case the
  // generator should release a number of permits equal to the result it adds to
  // the total. The collector should report the result when it has 100 permits.
  //
  // For bonus mark, use IO.race to collect the result when the first of 100
  // permits become available or 1 second has elapsed.

  def generate(ref: Ref[IO, Int], semaphore: Semaphore[IO]): IO[Unit] = {
    val process =
      smallRandomSleep
        .map(_ => random.nextInt(10))
        .flatMap { i =>
          ref.update(_ + i) *> semaphore.releaseN(i)
        }
        .replicateA_(100)

    List.fill(5)(process).parSequence.void
  }

  def collector(ref: Ref[IO, Int], semaphore: Semaphore[IO]): IO[Unit] = {
    IO.race(
      semaphore.acquireN(100) *> ref.get,
      IO.sleep(1.second) *> ref.get
    ).flatMap {
      case Left(value) => IO.println(s"Collected after 100 permits: $value")
      case Right(value) => IO.println(s"Collected after 1 second: $value")
    }
  }

  val run: IO[Unit] =
    (Ref.of[IO, Int](0), Semaphore[IO](0)).tupled.flatMap { case (ref, semaphore) =>
      (generate(ref, semaphore), collector(ref, semaphore)).parTupled
    }.void

}
