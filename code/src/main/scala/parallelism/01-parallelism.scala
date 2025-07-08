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
import cats.syntax.all.*
import org.openjdk.jmh.infra.Blackhole

import scala.concurrent.duration.FiniteDuration

// The goal of these exercises is to get comfortable with the basic tools for
// parallelism in IO, such as parMapN.
object Parallelism extends IOApp.Simple {
  // 0. This object extends IOApp.Simple. This allows us to run the object. It
  // expects an IO with the name `run` which describes the program it will run.
  // So replace `run` below with whatever you want to see your programs in
  // action.

  // How does parMap work? It's an instance of the Parallel type class.

  // Despite it's name, Parallel is not about running code in parallel. It's just about having a different
  // implementation of mapN and friends with different semantics.
  // For IO these different semantics are, coincidently, about parallelism,
  // but you can call parMapN on Either and get something else.

  val failed1: Either[List[String], Int] = Left(List("Oh no! I failed!"))
  val failed2: Either[List[String], Int] = Left(List("Oh no! I also failed!"))

  val mapNOnEithers: Either[List[String], Int] = (failed1, failed2).mapN(_ + _)
  val parMapNOnEithers: Either[List[String], Int] = (failed1, failed2).parMapN(_ + _)

   //val run = (IO(println("Left!")), IO(println("Right!"))).parTupled.flatMap(IO.println) // ((), ())

  val run: IO[Unit] = {
    IO.println(mapNOnEithers) *>
    IO.println(parMapNOnEithers)
  }

  // 1. Blackhole.consumeCPU consumes CPU cycles, and so is a useful way to
  // create tasks that take up enough time we can actually see the benefits of
  // parallelism. Wrap it up in an IO so we can use it in our IO based programs.
  //
  // This is an example call to Blackhole.consumeCPU. The parameter is the
  // number of "tokens", which determines how much CPU is consumed. It needs to
  // be quite high to have measurable results in what we'll do later.
  val consume: IO[Unit] = IO(Blackhole.consumeCPU(999999999L))

  // 2. Using the `timed` method, calculate the time it takes to run consume.
  // val run: IO[Unit] = consume.timed.flatMap((f, _) => IO.println(f.toMillis)) // 1924 millis

  // 3. Now time how long it takes to run consume twice, first sequentially and
  // then in parallel. What, if any, speedup does parallelism get in this case?
//  val run: IO[Unit] = {
//    val sequential: IO[Unit] = (consume, consume).mapN((_, _) => ()).timed
//      .flatMap(IO.println)
//
//    val parallel: IO[Unit] = (consume, consume).parMapN((_, _) => ()).timed
//      .flatMap(IO.println)
//
//    sequential // 3835 millis
//    parallel // 1948 millis
//  }

  // 4. Time how long it takes to run the list of IOs below, first sequentially
  // and then in parallel. You should be able to solve each task with only two
  // method calls, one of which is `timed`. What, if any, speedup do you see in
  // this case?
  val listOfConsume: List[IO[Unit]] = List.fill(24)(consume)
//  val run: IO[Unit] = {
//    val sequence = listOfConsume.sequence.timed.flatMap((f, _) => IO.println(f.toSeconds))
//
//    val parallel = listOfConsume.parSequence.timed.flatMap((f, _) => IO.println(f.toSeconds))
//
//    sequence // 45s
//    parallel // 6s
//  }

  // 5. Convert the list of tokens below into IOs wrapping calls to `consumeCPU`
  // and then time how long they take sequentially and in parallel. As before,
  // you should be able to achieve this in two method calls where one is
  // `timed`.
  val listofTokens = {
    List.tabulate(32)(i => Math.pow(2.toDouble, i.toDouble).toLong)
  }

//  val run: IO[Unit] = {
//    val sequence = listofTokens.traverse(_ => consume).timed.flatMap((f, _) => IO.println(f.toSeconds))
//
//    val parallel = listofTokens.parTraverse(_ => consume).timed.flatMap((f, _) => IO.println(f.toSeconds))
//
//    sequence // 61s
//    parallel // 8s
//  }
}
