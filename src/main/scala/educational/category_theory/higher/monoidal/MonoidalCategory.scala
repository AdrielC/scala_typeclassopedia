package educational.category_theory.higher.monoidal

import educational.category_theory.Category
import educational.category_theory.CategoryInstances.Function1Cat
import educational.category_theory.two.Bifunctor

/**
  * Monoidal Categories based on Category of Scala types and functions
  *
  * Edward Kmett Discrimination is Wrong: Improving Productivity
  * http://yowconference.com.au/slides/yowlambdajam2015/Kmett-DiscriminationIsWrong.pdf
  */
object MonoidalCategory {

  trait MonoidalCategory[:=>[_,_],⊗[_, _], I]
    extends Category[:=>] {

    def tensor: Bifunctor[⊗]
    def ρ[A]    (fa: A⊗I): A
    def ρ_inv[A](a:  A):   A⊗I

    def λ[A]      (fa: I⊗A): A
    def λ_inv[A,B](a:  A):   I⊗A

    def α[A,B,C](    fa: (A⊗B)⊗C): A⊗(B⊗C)
    def α_inv[A,B,C](fa: A⊗(B⊗C)): (A⊗B)⊗C
  }

  trait MonoidalCategoryLaws[:=>[_,_],⊗[_, _], I]
    extends MonoidalCategory[:=>,⊗, I] {

    def triangleEquations[A, B](fa: (A⊗I)⊗B): Boolean = {
      //               ρ[A] ⊗ id[B]
      // (A ⊗ I) ⊗ B ----------------> A ⊗ B
      val v1: A⊗B = tensor.bimap(ρ[A],identity[B])(fa)

      //              α[A,I,B]
      // (A ⊗ I) ⊗ B ---------->  A ⊗ (I ⊗ B)
      val w1: A⊗(I⊗B) = α[A,I,B](fa)

      //               id[A] ⊗ λ[B]
      // A ⊗ (I ⊗ B) ---------------> A ⊗ B
      val w2: A⊗B = tensor.bimap(identity[A],λ[B])(w1)

      v1 == w2
    }

    def pentagonEquations[A,B,C,D](fa: ((A⊗B)⊗C)⊗D): Boolean = {
      //                    α[A,B,C] ⊗ 1D
      // ((A ⊗ B) ⊗ C) ⊗ D ---------------> (A ⊗ (B ⊗ C)) ⊗ D
      val v1: (A⊗(B⊗C))⊗D = tensor.bimap(α[A,B,C],identity[D])(fa)

      //                    α[A,B⊗C,D]
      // (A ⊗ (B ⊗ C)) ⊗ D ------------> A ⊗ ((B ⊗ C) ⊗ D)
      val v2: A⊗((B⊗C)⊗D) = α[A,B⊗C,D](v1)

      //                    1A ⊗ α[B,C,D]
      // A ⊗ ((B ⊗ C) ⊗ D) ---------------> A ⊗ (B ⊗ (C ⊗ D))
      val v3: A⊗(B⊗(C⊗D)) = tensor.bimap(identity[A],α[B,C,D])(v2)

      //                     α[A⊗B,C,D]
      // ((A ⊗ B) ⊗ C) ⊗ D -------------> (A ⊗ B) ⊗ (C ⊗ D)
      val w1: (A⊗B)⊗(C⊗D) = α[A⊗B,C,D](fa)

      //                      α[A,B,C⊗D]
      // (A ⊗ B) ⊗ (C ⊗ D) -------------> A ⊗ (B ⊗ (C ⊗ D))
      val w2: A⊗(B⊗(C⊗D)) = α[A,B,C⊗D](w1)

      v3 == w2
    }
  }

val tupleBifunctor: Bifunctor[Tuple2] = new Bifunctor[Tuple2] {
  override def bimap[A,B,C,D](f: A => B, g: C => D): Tuple2[A,C] => Tuple2[B,D] =
    { case (a,c) => (f(a), g(c)) }
}

  trait TupleMc
    extends MonoidalCategory[Function1,Tuple2,Unit]
    with Function1Cat {

    val tensor: Bifunctor[Tuple2] = tupleBifunctor
    def ρ[A](fa: (A, Unit)): A = fa._1
    def ρ_inv[A](a: A): (A, Unit) = (a, ())
    def λ[A](fa: (Unit, A)): A = fa._2
    def λ_inv[A, B](a: A): (Unit, A) = ((), a)
    def α[A, B, C](fa: ((A, B), C)): (A, (B, C)) = fa match {case ((a,b),c) => (a, (b, c)) }
    def α_inv[A, B, C](fa: (A, (B, C))): ((A, B), C) = fa match {case (a,(b,c)) => ((a, b), c) }
  }

  val productMonoidalCategory: MonoidalCategory[Function1,Tuple2,Unit] = new TupleMc{}

  type Void <: Nothing

  val eitherBifunctor: Bifunctor[Either] = new Bifunctor[Either] {
    override def bimap[A, B, C, D](f: A => B, g: C => D): Either[A, C] => Either[B, D] = {
      case Left(a) => Left(f(a))
      case Right(c) => Right(g(c))
    }
  }

  trait Function1EitherMc
    extends MonoidalCategory[Function1,Either,Void]
      with Function1Cat {

    val tensor: Bifunctor[Either] = eitherBifunctor
    def ρ[A](fa: Either[A, Void]): A = fa match { case Left(a) => a }
    def ρ_inv[A](a: A): Either[A, Void] = Left(a)
    def λ[A](fa: Either[Void, A]): A = fa match { case Right(a) => a }
    def λ_inv[A, B](a: A): Either[Void, A] = Right(a)
    def α[A, B, C](fa: Either[Either[A, B], C]): Either[A, Either[B, C]] =
      fa match {
        case Left(Left(a)) => Left(a)
        case Left(Right(b)) => Right(Left(b))
        case Right(c) => Right(Right(c))
      }
    def α_inv[A, B, C](fa: Either[A, Either[B, C]]): Either[Either[A, B], C] = fa match {
      case Left(a) => Left(Left(a))
      case Right(Left(b)) => Left(Right(b))
      case Right(Right(c)) => Right(c)
    }
  }

  val coproductMonoidalCategory: MonoidalCategory[Function1,Either,Void] = new Function1EitherMc {}

  trait BraidedMonoidalCategory[:=>[_,_],⊗[_, _], I]
    extends MonoidalCategory[:=>,⊗, I] {

    def braiding[A,B](a: A⊗B): B⊗A
  }

  trait BraidedMonoidalCategoryLaws[:=>[_,_],⊗[_, _], I]
    extends BraidedMonoidalCategory[:=>,⊗, I]
      with MonoidalCategoryLaws[:=>,⊗, I] {

    def hexagonEquations[A,B,C,D](fa: ((A⊗B)⊗C)⊗D): Boolean = {
      //                      α[A,B,C] ⊗ 1D
      // ((A ⊗ B) ⊗ C) ⊗ D -----------------> (A ⊗ (B ⊗ C)) ⊗ D
      val v1: (A⊗(B⊗C))⊗D = tensor.bimap(α[A,B,C],identity[D])(fa)
      //                     α[A,B⊗C,D]
      // (A ⊗ (B ⊗ C)) ⊗ D ------------> A ⊗ ((B ⊗ C) ⊗ D)
      val v2: A⊗((B⊗C)⊗D) = α[A,B⊗C,D](v1)
      //                    1A ⊗ α[B,C,D]
      // A ⊗ ((B ⊗ C) ⊗ D) ------------------> A ⊗ (B ⊗ (C ⊗ D))
      val v3: A⊗(B⊗(C⊗D)) = tensor.bimap(identity[A],α[B,C,D])(v2)

      //                     α[A⊗B,C,D]
      // ((A ⊗ B) ⊗ C) ⊗ D -------------> (A ⊗ B) ⊗ (C ⊗ D)
      val w1: ((A⊗B)⊗(C⊗D)) = α[A⊗B,C,D](fa)
      //                     α[A,B,C⊗D]
      // (A ⊗ B) ⊗ (C ⊗ D) ------------> A ⊗ (B ⊗ (C ⊗ D))
      val w2: A⊗(B⊗(C⊗D)) = α[A,B,C⊗D](w1)

      v3 == w2
    }
  }

  val productBraidedMonoidalCategory: BraidedMonoidalCategory[Function1,Either,Void] =
    new BraidedMonoidalCategory[Function1,Either,Void]
      with Function1EitherMc {

      def braiding[A,B](a: Either[A,B]): Either[B,A] = a.swap
    }

  val tupleBraidedMonoidalCategory: BraidedMonoidalCategory[Function1,Tuple2,Unit] =
    new BraidedMonoidalCategory[Function1,Tuple2,Unit]
      with TupleMc {

      def braiding[A,B](a: (A,B)): (B,A) = a.swap
    }


  trait SymmetricMonoidalCategoryLaws[:=>[_,_],⊗[_, _], I]
    extends BraidedMonoidalCategory[:=>,⊗, I]
      with BraidedMonoidalCategoryLaws[:=>,⊗, I] {

    def hexagonEquations[A,B](fa: A⊗B): Boolean = {
      braiding(braiding(fa)) == fa
    }
  }
}
