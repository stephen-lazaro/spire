package spire.math

import spire.syntax.nroot._

// #bits -> x, the numerator of n-bit rational:  x / 2^n
case class CR(f: Int => SafeLong) { x =>

  import CR.{roundUp}

  def toRational(p: Int): Rational =
    Rational(x.f(p), SafeLong.two.pow(p))

  def toRational: Rational =
    Rational(x.f(CR.bits), SafeLong.two.pow(CR.bits))

  override def equals(y: Any): Boolean = y match {
    case y: CR =>
      val a = x eqv y
      val b = x.toString == y.toString
      (a, b) match {
        case (true, true) =>
          true
        case (true, false) =>
          println(s"warning, eqv is true, but strings differ: ${x.toString} and ${y.toString}")
          true
        case (false, false) =>
          false
        case (false, true) =>
          println(s"warning, eqv is false, but strings are same: ${x.toString} and ${y.toString}")
          false
      }
    case y => toRational.equals(y)
  }

  def eqv(y: CR): Boolean = (x - y).f(CR.bits) == SafeLong.zero
  // def eqv(y: CR): Boolean = 
  //   x.f(CR.bits) == y.f(CR.bits)

  def compare(y: CR): Int = (x - y).f(CR.bits).signum

  def min(y: CR): CR = CR(p => x.f(p) min y.f(p))
  def max(y: CR): CR = CR(p => x.f(p) max y.f(p))
  def abs(): CR = CR(p => x.f(p).abs)

  def signum(): Int = x.f(CR.bits).signum
  def unary_-(): CR = CR(p => -x.f(p))

  def reciprocal(): CR = {
    def xyz(i: Int): Int = if (SafeLong.three <= x.f(i).abs) i else xyz(i + 1)
    CR({p =>
      val s = xyz(0)
      roundUp(Rational(SafeLong.two.pow(2 * p + 2 * s + 2), x.f(p + 2 * s + 2)))
    })
  }

  def +(y: CR): CR =
    CR(p => roundUp(Rational(x.f(p + 2) + y.f(p + 2), 4)))

  def -(y: CR): CR = x + (-y)

  def *(y: CR): CR = {
    CR({p =>
      val x0 = x.f(0).abs + 2
      val y0 = y.f(0).abs + 2
      val sx = CR.sizeInBase(x0, 2) + 3
      val sy = CR.sizeInBase(y0, 2) + 3
      roundUp(Rational(x.f(p + sy) * y.f(p + sx), SafeLong.two.pow(p + sx + sy)))
    })
  }

  def pow(k: Int): CR = {
    def loop(b: CR, k: Int, extra: CR): CR =
      if (k == 1)
        b * extra
      else
        loop(b * b, k >>> 1, if ((k & 1) == 1) b * extra else extra)

    if (k < 0) {
      reciprocal.pow(-k)
    } else if (k == 0) {
      CR.one
    } else if (k == 1) {
      this
    } else {
      loop(x, k - 1, x)
    }
  }

  def /(y: CR): CR = x * y.reciprocal

  def %(y: CR): CR = CR({ p => 
    val d = x / y
    val s = d.f(2)
    val d2 = if (s >= 0) d.floor else d.ceil
    (x - d2 * y).f(p)
  })

  def /~(y: CR): CR = CR({ p => 
    val d = x / y
    val s = d.f(2)
    val d2 = if (s >= 0) d.floor else d.ceil
    d2.f(p)
  })

  def ceil(): CR = CR({ p =>
    val n = x.f(p)
    val t = SafeLong.two.pow(p)
    val m = n % t
    if (m == 0) n else n + t - m
  })

  def floor(): CR = CR({ p =>
    val n = x.f(p)
    val t = SafeLong.two.pow(p)
    val m = n % t
    n - m
  })

  def round: CR = CR({ p =>
    val n = x.f(p)
    val t = SafeLong.two.pow(p)
    val h = t / 2
    val m = n % t
    if (m < h) n - m else n - m + t
  })

  def isWhole(): Boolean = {
    val n = x.f(CR.bits)
    val t = SafeLong.two.pow(CR.bits)
      (n % t) == 0
  }

  def sqrt(): CR = CR(p => x.f(p * 2).sqrt)
  def nroot(k: Int): CR = CR(p => x.f(p * k).nroot(k))

  def fpow(r: Rational): CR =
    CR({ p =>
      val n = r.numerator
      val d = r.denominator
      if (n > Int.MaxValue || d > Int.MaxValue) sys.error("sorry: %s" format r.toString)
      x.pow(n.toInt).nroot(d.toInt).f(p)
    })

  // a bit hand-wavy
  def fpow(y: CR): CR =
    CR({ p =>
      x.fpow(Rational(y.f(p), SafeLong.two.pow(p))).f(p)
    })

  override def toString: String =
    getString(CR.digits)

  def getString(d: Int): String = {
    val b = CR.digitsToBits(d)
    val r = Rational(x.f(b) * SafeLong.ten.pow(d), SafeLong.two.pow(b))
    val m = roundUp(r)
    val (sign, str) = m.signum match {
      case -1 => ("-", m.abs.toString)
      case 0 => ("", "0")
      case 1 => ("", m.toString)
    }
    val i = str.length - d
    val s = if (i > 0) {
      sign + str.substring(0, i) + "." + str.substring(i)
    } else {
      sign + "0." + ("0" * -i) + str
    }
    s.replaceAll("0+$", "").replaceAll("\\.$", "")
  }
}

object CR {
  import spire.algebra._

  val zero = CR(p => SafeLong.zero)
  val one = CR(p => SafeLong.two.pow(p))
  val two = CR(p => SafeLong.two.pow(p + 1))
  val four = CR(p => SafeLong.two.pow(p + 2))

  def apply(n: Long): CR = CR(p => SafeLong.two.pow(p) * n)
  def apply(n: BigInt): CR = CR(p => SafeLong.two.pow(p) * n)
  def apply(n: SafeLong): CR = CR(p => SafeLong.two.pow(p) * n)
  def apply(n: Rational): CR = CR(p => roundUp(Rational(2).pow(p) * n))
  def apply(n: Double): CR = CR(Rational(n))
  def apply(n: BigDecimal): CR = CR(Rational(n))
  def apply(s: String): CR = CR(Rational(s))

  lazy val pi = CR(16) * atan(CR(Rational(1, 5))) - CR.four * atan(CR(Rational(1, 239)))

  lazy val e = exp(CR.one)

  def log(x: CR): CR = {
    val t = x.f(2)
    val n = sizeInBase(t, 2) - 3
    if (t < 0) sys.error("log of negative number")
    else if (t < 4) -log(x.reciprocal)
    else if (t < 8) logDr(x)
    else logDr(div2n(x, n)) + CR(n) * log2
  }

  def exp(x: CR): CR = {
    val u = x / log2
    val n = u.f(0)
    val s = x - CR(n) * log2
    if (!n.isValidInt) sys.error("sorry")
    else if (n < 0) div2n(expDr(s), -n.toInt)
    else if (n > 0) mul2n(expDr(s), n.toInt)
    else expDr(s)
  }

  def sin(x: CR): CR = {
    val z = x / piBy4
    val s = roundUp(Rational(z.f(2), 4))
    val y = x - piBy4 * CR(s)
    val m = (s % 8).toInt
    val n = if (m < 0) m + 8 else m
    n match {
      case 0 => sinDr(y)
      case 1 => sqrt1By2 * (cosDr(y) + sinDr(y))
      case 2 => cosDr(y)
      case 3 => sqrt1By2 * (cosDr(y) - sinDr(y))
      case 4 => -sinDr(y)
      case 5 => -sqrt1By2 * (cosDr(y) + sinDr(y))
      case 6 => -cosDr(y)
      case 7 => -sqrt1By2 * (cosDr(y) - sinDr(y))
    }
  }

  def cos(x: CR): CR = {
    val z = x / piBy4
    val s = roundUp(Rational(z.f(2), 4))
    val y = x - piBy4 * CR(s)
    val m = (s % 8).toInt
    val n = if (m < 0) m + 8 else m
    n match {
      case 0 => cosDr(y)
      case 1 => sqrt1By2 * (cosDr(y) - sinDr(y))
      case 2 => -sinDr(y)
      case 3 => -sqrt1By2 * (cosDr(y) + sinDr(y))
      case 4 => -cosDr(y)
      case 5 => -sqrt1By2 * (cosDr(y) - sinDr(y))
      case 6 => sinDr(y)
      case 7 => sqrt1By2 * (cosDr(y) + sinDr(y))
    }
  }

  def tan(x: CR): CR = sin(x) / cos(x)

  def atan(x: CR): CR = {
    val t = x.f(2)
    val xp1 = x + CR.one
    val xm1 = x - CR.one
    if (t < -5) atanDr(-x.reciprocal - piBy2)
    else if (t == -4) -piBy4 - atanDr(xp1 / xm1)
    else if (t < 4) atanDr(x)
    else if (t == 4) piBy4 + atanDr(xm1 / xp1)
    else piBy2 - atanDr(x.reciprocal)
  }

  def atan2(y: CR, x: CR): CR = CR({ p =>
    val sx = x.f(p).signum
    val sy = y.f(p).signum
    if (sx > 0) {
      atan(y / x).f(p)
    } else if (sy >= 0 && sx < 0) {
      (atan(y / x) + CR.pi).f(p)
    } else if (sy < 0 && sx < 0) {
      (atan(y / x) - CR.pi).f(p)
    } else if (sy > 0) {
      (CR.pi / CR.two).f(p)
    } else if (sy < 0) {
      (-CR.pi / CR.two).f(p)
    } else {
      sys.error("undefined")
    }
  })

  def asin(x: CR): CR = {
    val x0 = x.f(0)
    val s = (CR.one - x * x).sqrt
    if (x0 > 0) pi / CR.two - atan(s / x)
    else if (x0 == 0) atan(x / s)
    else atan(s / x) - pi / CR.two
  }

  def acos(x: CR): CR  = pi / CR.two - asin(x)

  def sinh(x: CR): CR = {
    val y = exp(x)
    (y - y.reciprocal) / CR.two
  }

  def cosh(x: CR): CR = {
    val y = exp(x)
    (y + y.reciprocal) / CR.two
  }

  def tanh(x: CR): CR = {
    val y = exp(x);
    val y2 = y.reciprocal
    (y - y2) / (y + y2)
  }

  def asinh(x: CR): CR = log(x + (x * x + CR.one).sqrt)
  def acosh(x: CR): CR = log(x + (x * x - CR.one).sqrt)
  def atanh(x: CR): CR = log((CR.one + x) / (CR.one - x)) / CR.two

  def digits: Int = 40
  def bits: Int = digitsToBits(digits)

  def digitsToBits(n: Int): Int =
    spire.math.ceil(n * (spire.math.log(10.0) / spire.math.log(2.0))).toInt + 4

  def sizeInBase(n: SafeLong, base: Int): Int = {
    def loop(n: SafeLong, acc: Int): Int = if (n <= 1) acc + 1 else loop(n / base, acc + 1)
    loop(n.abs, 0)
  }

  def roundUp(r: Rational): SafeLong = SafeLong((r + Rational(1, 2)).floor.toBigInt)

  def div2n(x: CR, n: Int): CR =
    CR(p => if (p >= n) x.f(p - n) else roundUp(Rational(x.f(p), SafeLong.two.pow(n))))

  def mul2n(x: CR, n: Int): CR =
    CR(p => x.f(p + n))

  lazy val piBy2 = div2n(pi, 1)

  lazy val piBy4 = div2n(pi, 2)

  lazy val log2 = div2n(logDrx(CR.two.reciprocal), 1)

  lazy val sqrt1By2 = CR.two.reciprocal.sqrt

  def accumulate(total: SafeLong, xs: Stream[SafeLong], cs: Stream[Rational]): SafeLong = {
    (xs, cs) match {
      case (_, Stream.Empty) => total
      case (Stream.Empty, _) => sys.error("nooooo")
      case (x #:: xs, c #:: cs) =>
        val t = roundUp(c * Rational(x))
        if (t == 0) total else accumulate(total + t, xs, cs)
    }
  }

  private[spire] def powerSeries(ps: Stream[Rational], terms: Int => Int, x: CR): CR = {
    CR({p =>
      val t = terms(p)
      val l2t = 2 * sizeInBase(SafeLong(t) + 1, 2) + 6
      val p2 = p + l2t
      val xr = x.f(p2)
      val xn = SafeLong.two.pow(p2)
      if (xn == 0) sys.error("oh no")
      def g(yn: SafeLong): SafeLong = roundUp(Rational(yn * xr, xn))
      val num = accumulate(SafeLong.zero, Stream.iterate(xn)(g), ps.take(t))
      val denom = SafeLong.two.pow(l2t)
      roundUp(Rational(num, denom))
    })
  }

  private[spire] def accSeq(f: (Rational, SafeLong) => Rational): Stream[Rational] = {
    def loop(r: Rational, n: SafeLong): Stream[Rational] =
      r #:: loop(f(r, n), n + 1)
    loop(Rational.one, SafeLong.one)
  }

  def expDr(x: CR): CR =
    powerSeries(accSeq((r, n) => r / n), n => n, x)

  def logDr(x: CR): CR = {
    val y = (x - CR.one) / x
    y * logDrx(y)
  }

  def logDrx(x: CR): CR = {
    powerSeries(Stream.from(1).map(n => Rational(1, n)), _ + 1, x)
  }

  def sinDr(x: CR): CR =
    x * powerSeries(accSeq((r, n) => -r * Rational(1, 2*n*(2*n+1))), n => n, x * x)

  def cosDr(x: CR): CR =
    powerSeries(accSeq((r, n) => -r * Rational(1, 2*n*(2*n-1))), n => n, x * x)

  def atanDr(x: CR): CR = {
    val y = x * x + CR(1)
    (x / y) * atanDrx((x * x) / y)
  }

  def atanDrx(x: CR): CR =
    powerSeries(accSeq((r, n) => r * (Rational(2*n, 2*n + 1))), _ + 1, x)

  implicit val algebra = new Fractional[CR] with Order[CR] with Signed[CR] with Trig[CR] {
    def abs(x: CR): CR = x.abs
    def signum(x: CR): Int = x.signum

    override def eqv(x: CR, y: CR): Boolean = x eqv y
    def compare(x: CR, y: CR): Int = x compare y

    def zero: CR = CR.zero
    def one: CR = CR.one
    def negate(x: CR): CR = -x
    def plus(x: CR, y: CR): CR = x + y
    override def minus(x: CR, y: CR): CR = x - y
    def times(x: CR, y: CR): CR = x * y

    def gcd(x: CR, y: CR): CR = x min y //fixme
    def quot(x: CR, y: CR): CR = x / y //fixme
    def mod(x: CR, y: CR): CR = CR.zero //fixme

    override def reciprocal(x: CR): CR = x.reciprocal
    def div(x: CR, y: CR): CR = x / y

    override def sqrt(x: CR): CR = x.sqrt
    def nroot(x: CR, k: Int): CR = x.nroot(k)
    def fpow(x: CR, y: CR): CR = x fpow y

    def acos(a: CR): CR = CR.acos(a)
    def asin(a: CR): CR = CR.asin(a)
    def atan(a: CR): CR = CR.atan(a)
    def atan2(y: CR, x: CR): CR = CR.atan2(y, x)
    def cos(a: CR): CR = CR.cos(a)
    def cosh(x: CR): CR = CR.cosh(x)
    def e: CR = CR.e
    def exp(x: CR): CR = CR.exp(x)
    def expm1(x: CR): CR = CR.exp(CR.one) - CR.one
    def log(x: CR): CR = CR.log(x)
    def log1p(x: CR): CR = CR.log(CR.one + x)
    def pi: CR = CR.pi
    def sin(x: CR): CR = CR.sin(x)
    def sinh(x: CR): CR = CR.sinh(x)
    def tan(x: CR): CR = CR.tan(x)
    def tanh(x: CR): CR = CR.tanh(x)
    def toDegrees(a: CR): CR = a / (CR.two * CR.pi) * CR(360)
    def toRadians(a: CR): CR = a / CR(360) * (CR.two * CR.pi)

    def ceil(x: CR): CR = x.ceil
    def floor(x: CR): CR = x.floor
    def isWhole(x: CR): Boolean = x.isWhole
    def round(x: CR): CR = x.round

    def toRational(x: CR): Rational = x.toRational
    def toDouble(x: CR): Double = x.toRational.toDouble
    def toBigDecimal(x: CR): BigDecimal = x.toRational.toBigDecimal
    def toBigInt(x: CR): BigInt = x.toRational.toBigInt
    def toByte(x: CR): Byte = x.toRational.toByte
    def toFloat(x: CR): Float = x.toRational.toFloat
    def toInt(x: CR): Int = x.toRational.toInt
    def toLong(x: CR): Long = x.toRational.toLong
    def toNumber(x: CR): Number = Number(x.toRational)
    def toShort(x: CR): Short = x.toRational.toShort
    def toString(x: CR): String = x.toString

    def toType[B](x: CR)(implicit ev: ConvertableTo[B]): B =
      ev.fromRational(x.toRational)

    def fromBigDecimal(n: BigDecimal): CR = CR(n)
    def fromBigInt(n: BigInt): CR = CR(n)
    def fromByte(n: Byte): CR = CR(n)
    def fromFloat(n: Float): CR = CR(n)
    def fromLong(n: Long): CR = CR(n)
    def fromRational(n: Rational): CR = CR(n)
    def fromShort(n: Short): CR = CR(n)

    def fromType[B](b: B)(implicit ev: ConvertableFrom[B]): CR =
      CR(ev.toRational(b))
  }
}