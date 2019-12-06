package profunctor

import educational.Functor

/** Lift backward Functor into Profunctor */
case class Costar[F[_],D,C](runCostar: F[D] => C)

object CostarInstances { // https://hackage.haskell.org/package/profunctors-5.3/docs/Data-Profunctor.html#i:Costar
  def profunctor[F[_]](FF: Functor[F]): Profunctor[Costar[F, ?, ?]] = new Profunctor[Costar[F, ?, ?]] {
    def dimap[A, B, C, D](ab: A => B, cd: C => D): Costar[F, B, C] => Costar[F, A, D] = fbc =>
      Costar{ fa =>
        val v: F[B] = FF.map(fa)(ab)
        val c: C = fbc.runCostar(v)
        cd(c)
      }
  }

  // TODO more instances
}
