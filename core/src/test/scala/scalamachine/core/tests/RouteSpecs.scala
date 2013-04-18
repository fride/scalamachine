package scalamachine.core.tests

import org.specs2._
import scalamachine.core.dispatch.Route._
import org.scalacheck.{Arbitrary, Prop, Gen}
import Prop._
import scalamachine.core._
import dispatch._

class RouteSpecs extends Specification with ScalaCheck { override def is =
  "Routes".title                                                                    ^
  """
  Routes are a partial function from a request path
  to a resource
  """                                                                               ^
                                                                                    p^
  "Matching Paths"                                                                  ^
    "Routes with no data parts"                                                     ^
      "if the route does not end in a star term"                                    ^
        "path matches iif it each token is equal to each string part"               ! AllStringRoutesNoStar.testExactMatchingPath ^
        "path does not match if >= one token is not equal to a string part"         ! AllStringRoutesNoStar.testPartDoesntMatch ^
        "path does not match if it has less tokens than route parts"                ! AllStringRoutesNoStar.testLessTokensThanParts ^
        "path does not match if it has more tokens than route parts"                ! AllStringRoutesNoStar.testMoreTokensThanParts ^
        "path data always has as empty dispatch path string"                        ! AllStringRoutesNoStar.testDispPathAlwaysEmpty ^
        "path data always has an empty path tokens Seq"                             ! AllStringRoutesNoStar.testPathTokensAlwaysEmpty ^p^
      "if the route does end in a star term"                                        ^
        "path matches if each token is equal to each string part"                   ! AllStringRoutesWithStar.testExactStartingWithMatch ^
        "path matches with tokens equal to parts even with leftover tokens"         ! AllStringRoutesWithStar.testLeftoverTokensStillMatch ^
        "path does not match if >= one token is not equal to a string part"         ! AllStringRoutesWithStar.testPartDoesntMatch ^
        "path does not match if there are less tokens than route parts"             ! AllStringRoutesWithStar.testLessTokensThanParts ^
        "path data has an empty dispatch string if num tokens == num route terms"   ! AllStringRoutesWithStar.testDispPathAlwaysEmpty ^
        "path data has left over tokens seperated by slash as dispatch string"      ! AllStringRoutesWithStar.testLeftoverTokensDispPath ^
        "path data has empty path tokens Seq if num tokens == num route terms"      ! AllStringRoutesWithStar.testLeftoverTokensPathTokens ^
        "path data has left over tokens in path tokens Seq with order preserved"    ! AllStringRoutesWithStar.testPathTokensAlwaysEmpty ^p^
      "path data always have empty path info map"                                   ! testStringRoutesAlwaysHaveEmptyPathInfo ^p^
    "Routes with data parts"                                                        ^
      "if the route does not end in a star term"                                    ^
        "if the route contains string terms"                                        ^
          "path matches iif each token that has corresponding string part matches"  ! MixedPathNoStar.testMatchesIIFStringPartsMatch ^
          "path does not match if >= one token does not match corresponding string" ! MixedPathNoStar.testStringPartDoesntMatch ^
          "path does not match if it has less tokens than route parts"              ! MixedPathNoStar.testLessTokensThanParts ^
          "path does not match if has more tokens than route parts"                 ! MixedPathNoStar.testMoreTokensThanParts ^
          "path data always has as empty dispatch path string"                      ! MixedPathNoStar.testDispPathAlwaysEmpty ^
          "path data always has an empty path tokens Seq"                           ! MixedPathNoStar.testPathTokensAlwaysEmpty ^p^
        "if the route does not contain string terms"                                ^
          "path matches only if the number of tokens equal the number of terms"     ! AllDataRouteNoStar.testEqualLengths ^
          "the path does not match otherwise"                                       ! AllDataRouteNoStar.testUnequalLengths ^
          "path data always has as empty dispatch path string"                      ! AllDataRouteNoStar.testDispPathAlwaysEmpty ^
          "path data always has an empty path tokens Seq"                           ! AllDataRouteNoStar.testPathTokensAlwaysEmpty ^p^p^
      "if the route ends in a star term"                                            ^
        "if the route contains string terms"                                        ^
          "path matches if each token thas has corresponding string part matches"   ! MixedPathWithStar.testMatchesIIFStringPartsMatch ^
          "path matches with tokens equal to string parts even with leftover tokens"! MixedPathWithStar.testMatchesWithLeftoverTokens ^
          "path does not match if >= one token is not equal to corresponding string"! MixedPathWithStar.testStringPartDoesntMatch ^
          "path does not match if there are less tokens than route parts"           ! MixedPathWithStar.testLessTokensThanParts ^
          "path data has an empty dispatch string if num tokens == num route terms" ! MixedPathWithStar.testDispPathAlwaysEmpty ^
          "path data has left over tokens seperated by slash as dispatch string"    ! MixedPathWithStar.testLeftoverTokensDispPath ^
          "path data has empty path tokens Seq if num tokens == num route terms"    ! MixedPathWithStar.testPathTokensAlwaysEmpty  ^
          "path data has left over tokens in path tokens Seq with order preserved"  ! MixedPathWithStar.testLeftoverTokensPathTokens ^p^
        "if the route does not contain string terms"                                ^
          "path matches only if the number of tokens is >= number of terms"         ! AllDataRouteWithStar.testWithLeftoverTokens ^
          "the path does not match otherwise"                                       ! AllDataRouteWithStar.testLessTokensThanParts ^
          "path data has an empty dispatch string if num tokens == num route terms" ! AllDataRouteWithStar.testDispPathAlwaysEmpty ^
          "path data has left over tokens seperated by slash as dispatch string"    ! AllDataRouteWithStar.testLeftoverTokensDispPath ^
          "path data has empty path tokens Seq if num tokens == num route terms"    ! AllDataRouteWithStar.testPathTokensAlwaysEmpty ^
          "path data has left over tokens in path tokens Seq with order preserved"  ! AllDataRouteWithStar.testLeftoverTokensPathTokens ^p^p^
      "path data's path info contains key corresponding to token for each data part"! testMixedRoutesPathInfo ^
                                                                                    endp^
  "Matching Hosts"                                                                  ^
    "if host route does not begin with a star term"                                 ^
      "matches iif each token with corresponding string part matches"               ! MixedHostNoStar.testMatchesIIFStringPartsMatch ^
      "does not match if tokens.length < parts.length"                              ! MixedHostNoStar.testLessTokensThanParts ^
      "does not match if tokens.length > parts.length"                              ! MixedHostNoStar.testMoreTokensThanParts ^
      "does not match if >= 1 token does not match corresponding string part"       ! MixedHostNoStar.testStringPartDoesntMatch ^
      "host data always has empty dispatch subdomain string"                        ! MixedHostNoStar.testDispSubDomainAlwaysEmpty ^
      "host data always has empty subdomain tokens"                                 ! MixedHostNoStar.testSubdomainTokensAlwaysEmpty ^
      "host data has entry in host info for every data part"                        ! MixedHostNoStar.testHostInfo ^p^
    "if host route begins with a star term"                                         ^
      "matches iif each token with corresponding string part matches (lefovers ok)" ! MixedHostWithStar.testMatchesWithLeftoverTokens ^
      "does not match if tokens.length < parts.length"                              ! MixedHostWithStar.testLessTokensThanParts ^
      "does not match if >= 1 token does not match corresponding string part"       ! MixedHostWithStar.testStringPartDoesntMatch ^
      "host data has empty dispatch subdomain if tokens.length == parts.length"     ! MixedHostWithStar.testDispSubDomainAlwaysEmpty ^
      "host data has leftover tokens seperated by dots as dispatch subdomain"       ! MixedHostWithStar.testLeftoverTokensDispSubdomain ^
      "host data has empty dispatch tokens if tokens.length == parts.length"        ! MixedHostWithStar.testSubdomainTokensAlwaysEmpty ^
      "host data has left tokens as dispatch tokens w/ order preserved"             ! MixedHostWithStar.testLeftoverTokensSubdomainTokens ^
      "host data has entry in host info for every data part"                        ! MixedHostWithStar.testHostInfo ^
                                                                                    endp^
  "Matching Hosts and Paths"                                                        ^
    "matches if host and path route matches given host and path tokens"             ! testHostAndPathRoute ^
    "does not match if host route does not match given host tokens"                 ! testHostAndPathHostNotMatching ^
    "does not match if path route does not match given path tokens"                 ! testHostAndPathPathNotMatching ^
                                                                                    endp^
  "Gaurds"                                                                          ^
    "if unguarded route matches"                                                    ^
      "if guard returns true, route matches"                                        ! testGuardTrue ^
      "if guard returns false, route does not match"                                ! testGuardFalse ^p^
    "if unguarded route does not match, guard is never run"                         ! testGuardNotCalledWhenNotDefined ^
                                                                                    end

  def testGuardTrue = check {
    (pathParts: List[String]) => {
      val route: Route = pathMatching(pathParts.map(routeToken(_))) guardedBy { _ => true } serve null
      route.isDefinedAt(ReqRespData(pathParts = pathParts)) must beTrue
    }
  }

  def testGuardFalse = check {
    (pathParts: List[String]) => {
      val route: Route = pathMatching(pathParts.map(routeToken(_))) guardedBy { _ => false } serve null
      route.isDefinedAt(ReqRespData(pathParts = pathParts)) must beFalse
    }
  }

  def testGuardNotCalledWhenNotDefined = forAll(nonEmptyTokens suchThat { _.size > 3}) {
    (pathParts: List[String]) => {
      val route: Route = pathMatching(pathParts.reverse.map(routeToken(_))) guardedBy { _ => throw new RuntimeException("I shouldn't be run") } serve null
      route.isDefinedAt(ReqRespData(pathParts = pathParts)) must not(throwA[RuntimeException])
    }
  }

  trait PathDataShared {
    def routeF: List[RoutePart] => Route
    def toPart: String => RoutePart

    def testDispPathAlwaysEmpty = check {
      (pathParts: List[String]) => routeF(pathParts.map(toPart(_))).apply(ReqRespData(pathParts = pathParts))._2.dispPath must beEmpty
    }

    def testPathTokensAlwaysEmpty = check {
      (pathParts: List[String]) => routeF(pathParts.map(toPart(_))).apply(ReqRespData(pathParts = pathParts))._2.pathTokens must beEmpty
    }

    def testLeftoverTokensDispPath = forAll(nonEmptyTokens, nonEmptyTokens) {
      (pathParts: List[String], additional: List[String]) =>
        routeF(pathParts.map(toPart(_))).apply(ReqRespData(pathParts = pathParts ++ additional))._2.dispPath must beEqualTo(additional.mkString("/"))
    }

    def testLeftoverTokensPathTokens = forAll(nonEmptyTokens, nonEmptyTokens) {
      (pathParts: List[String], additional: List[String]) =>
        routeF(pathParts.map(toPart(_))).apply(ReqRespData(pathParts = pathParts ++ additional))._2.pathTokens must containAllOf(additional).inOrder
    }
  }

  trait MixedTermsPathDataShared {
    this: MixedRoutesShared =>

    def testDispPathAlwaysEmpty = forAll(tokensAndDataPartIdxs) {
      (data: (List[String],Set[Int])) => {
        val (pathParts,dataIdxs) = data
        routeF(buildMixedRouteTerms(pathParts,dataIdxs)).apply(ReqRespData(pathParts = pathParts))._2.dispPath must beEmpty
      }
    }

    def testPathTokensAlwaysEmpty = forAll(tokensAndDataPartIdxs) {
      (data: (List[String],Set[Int])) => {
        val (pathParts,dataIdxs) = data
        routeF(buildMixedRouteTerms(pathParts,dataIdxs)).apply(ReqRespData(pathParts = pathParts))._2.pathTokens must beEmpty
      }
    }

  }

  trait MixedTermsHostDataShared {
    this: MixedRoutesShared =>

    def testDispSubDomainAlwaysEmpty = forAll(tokensAndDataPartIdxs) {
      (data: (List[String],Set[Int])) => {
        val (hostParts,dataIdxs) = data
        routeF(buildMixedRouteTerms(hostParts,dataIdxs)).apply(ReqRespData(hostParts = hostParts))._2.dispSubdomain must beEmpty
      }
    }

    def testSubdomainTokensAlwaysEmpty = forAll(tokensAndDataPartIdxs) {
      (data: (List[String],Set[Int])) => {
        val (hostParts,dataIdxs) = data
        routeF(buildMixedRouteTerms(hostParts,dataIdxs)).apply(ReqRespData(hostParts = hostParts))._2.subdomainTokens must beEmpty
      }
    }

    def testHostInfo = forAll(tokensAndDataPartIdxs) {
      (data: (List[String],Set[Int])) => {
        val (hostParts,dataIdxs) = data
        val terms = buildMixedRouteTerms(hostParts,dataIdxs)
        val route = routeF(terms)
        val expected = hostParts.zipWithIndex.filter(t => dataIdxs.contains(t._2)).map(tpl => (Symbol(tpl._1),tpl._1))
        route.apply(ReqRespData(hostParts = hostParts))._2.hostInfo must containAllOf(expected).only
      }
    }

  }
  
  trait AllStringsPathDataShared extends PathDataShared {
    override val toPart: String => RoutePart = routeToken(_)
  }
  
  trait AllDataPathDataShared extends PathDataShared {
    override def toPart: String => RoutePart = s => routeData(Symbol(s))
  }
  
  trait MixedRoutesShared  {
    def routeF: List[RoutePart] => Route

    def testMatchesIIFStringPartsMatch = forAll(tokensAndDataPartIdxs) {
      (data: (List[String],Set[Int])) => {
        val (tokens,dataIdxs) = data
        routeF(buildMixedRouteTerms(tokens,dataIdxs)).isDefinedAt(ReqRespData(hostParts = tokens, pathParts = tokens)) must beTrue
      }
    }

    def testLessTokensThanParts = forAll(tokensDataPartIdxsAndDropCount) {
      (data: (List[String],Set[Int],Int)) => {
        val (tokens, dataIdxs,dropCount) = data
        val finalParts = tokens.reverse.drop(dropCount).reverse
        routeF(buildMixedRouteTerms(tokens,dataIdxs))
          .isDefinedAt(ReqRespData(hostParts = finalParts, pathParts = finalParts)) must beFalse
      }
    }

    def testStringPartDoesntMatch = forAll(tokensDataPartIdxsAndChange) {
      (data: (List[String],Set[Int],Int,String)) => {
        val (tokens,dataIdxs,changeAt,changeTo) = data
        val changedParts = tokens.toBuffer
        changedParts.update(changeAt, changeTo)
        val finalParts = changedParts.toList
        routeF(buildMixedRouteTerms(tokens,dataIdxs)).isDefinedAt(ReqRespData(hostParts = finalParts, pathParts = finalParts)) must beFalse
      }
    }
       
  }

  object MixedPathWithStar extends MixedRoutesShared with MixedTermsPathDataShared {
    override val routeF: List[RoutePart] => Route = l => pathStartingWith(l) serve null

    def testMatchesWithLeftoverTokens = forAll(tokensAndDataPartIdxs, nonEmptyTokens) {
      (data: (List[String],Set[Int]), additional: List[String]) => {
        val (pathParts,dataIdxs) = data
        pathStartingWith(pathParts.zipWithIndex.map {
          case (s,idx) if dataIdxs.contains(idx) => routeData(Symbol(s))
          case (s,_) => routeToken(s)
        }).serve(null).isDefinedAt(ReqRespData(pathParts = pathParts ++ additional)) must beTrue
      }
    }

    def testLeftoverTokensDispPath = forAll(tokensAndDataPartIdxs, nonEmptyTokens) {
      (data: (List[String],Set[Int]), additional: List[String]) => {
        val (pathParts,dataIdxs) = data
        routeF(buildMixedRouteTerms(pathParts,dataIdxs)).apply(ReqRespData(pathParts = pathParts ++ additional))._2.dispPath must beEqualTo(additional.mkString("/"))
      }
    }

    def testLeftoverTokensPathTokens = forAll(tokensAndDataPartIdxs, nonEmptyTokens) {
      (data: (List[String],Set[Int]), additional: List[String]) => {
        val (pathParts,dataIdxs) = data
        routeF(buildMixedRouteTerms(pathParts,dataIdxs)).apply(ReqRespData(pathParts = pathParts ++ additional))._2.pathTokens must containAllOf(additional).inOrder
      }
    }
  }

  object MixedPathNoStar extends MixedRoutesShared with MixedTermsPathDataShared {
    override val routeF: List[RoutePart] => Route = l => pathMatching(l) serve null

    def testMoreTokensThanParts = forAll(tokensAndDataPartIdxs, nonEmptyTokens) {
      (data: (List[String],Set[Int]), additional: List[String]) => {
        val (pathParts,dataIdxs) = data
        pathMatching(buildMixedRouteTerms(pathParts,dataIdxs)).serve(null).isDefinedAt(ReqRespData(pathParts = pathParts ++ additional)) must beFalse
      }
    }

  }

  object MixedHostWithStar extends MixedRoutesShared with MixedTermsHostDataShared {
    override def routeF: List[RoutePart] => Route = l => hostEndingWith(l) serve null

    def testMatchesWithLeftoverTokens = forAll(tokensAndDataPartIdxs,Gen.containerOf[List,String](tok)) {
      (data: (List[String],Set[Int]), additional: List[String]) => {
        val (hostParts,dataIdxs) = data
        hostEndingWith(hostParts.zipWithIndex.map {
          case (s,idx) if dataIdxs.contains(idx) => routeData(Symbol(s))
          case (s,_) => routeToken(s)
        }).serve(null).isDefinedAt(ReqRespData(hostParts = additional ++ hostParts)) must beTrue
      }
    }

    def testLeftoverTokensDispSubdomain = forAll(tokensAndDataPartIdxs, nonEmptyTokens) {
      (data: (List[String],Set[Int]), additional: List[String]) => {
        val (pathParts,dataIdxs) = data
        routeF(buildMixedRouteTerms(pathParts,dataIdxs)).apply(ReqRespData(hostParts = additional ++ pathParts))._2.dispSubdomain must beEqualTo(additional.mkString("."))
      }
    }

    def testLeftoverTokensSubdomainTokens = forAll(tokensAndDataPartIdxs, nonEmptyTokens) {
      (data: (List[String],Set[Int]), additional: List[String]) => {
        val (pathParts,dataIdxs) = data
        routeF(buildMixedRouteTerms(pathParts,dataIdxs)).apply(ReqRespData(hostParts = additional ++ pathParts))._2.subdomainTokens must containAllOf(additional).inOrder
      }
    }


  }

  def testHostAndPathRoute = forAll(tokensAndDataPartIdxs,Gen.containerOf[List,String](tok)) {
    (data: (List[String],Set[Int]), additional: List[String]) => {
      val (tokens, idxs) = data
      val terms = buildMixedRouteTerms(tokens, idxs)
      val route: Route = hostEndingWith(terms) andPathStartingWith(terms) serve null
      route.isDefinedAt(ReqRespData(hostParts = additional ++ tokens, pathParts = tokens ++ additional)) must beTrue
    }
  }

  def testHostAndPathHostNotMatching = forAll(tokensAndDataPartIdxs) {
    (data: (List[String], Set[Int])) => {
      val (tokens, idxs) = data
      val terms = buildMixedRouteTerms(tokens, idxs)
      val badTokens = tokens.reverse
      val route: Route = hostMatching(terms) andPathMatching(terms) serve null
      route.isDefinedAt(ReqRespData(hostParts = badTokens, pathParts = tokens)) must beFalse
    }
  }

  def testHostAndPathPathNotMatching = forAll(tokensAndDataPartIdxs) {
    (data: (List[String], Set[Int])) => {
      val (tokens, idxs) = data
      val terms = buildMixedRouteTerms(tokens, idxs)
      val badTokens = tokens.reverse
      val route: Route = hostMatching(terms) andPathMatching(terms) serve null
      route.isDefinedAt(ReqRespData(hostParts = tokens, pathParts = badTokens)) must beFalse
    }
  }

  object MixedHostNoStar extends MixedRoutesShared with MixedTermsHostDataShared {
    override val routeF: List[RoutePart] => Route = l => hostMatching(l) serve null

    def testMoreTokensThanParts = forAll(tokensAndDataPartIdxs, nonEmptyTokens) {
      (data: (List[String],Set[Int]), additional: List[String]) => {
        val (tokens,dataIdxs) = data
        hostMatching(buildMixedRouteTerms(tokens,dataIdxs)).serve(null).isDefinedAt(ReqRespData(hostParts = additional ++ tokens)) must beFalse
      }
    }

  }
  
  def testMixedRoutesPathInfo = forAll(for { (a,b) <- tokensAndDataPartIdxs;c <- Arbitrary.arbitrary[Boolean] } yield (a,b,c)) {
    (data: (List[String],Set[Int],Boolean)) => {
      val (pathParts,dataIdxs,bool) = data
      val terms = buildMixedRouteTerms(pathParts,dataIdxs)
      val route = if (bool) pathMatching(terms) serve null else pathStartingWith(terms) serve null
      val expected = pathParts.zipWithIndex.filter(t => dataIdxs.contains(t._2)).map(tpl => (Symbol(tpl._1),tpl._1))
      route.apply(ReqRespData(pathParts = pathParts))._2.pathInfo must containAllOf(expected).only
    }
  }

  object AllDataRouteWithStar extends AllDataPathDataShared {
    override val routeF: List[RoutePart] => Route = l => pathStartingWith(l) serve null

    def testWithLeftoverTokens = forAll(nonEmptyTokens, nonEmptyTokens) {
      (pathParts: List[String], additional: List[String]) =>
        pathStartingWith(pathParts.map(s => routeData(Symbol(s)))).serve(null).isDefinedAt(ReqRespData(pathParts = pathParts ++ additional)) must beTrue
    }

    def testLessTokensThanParts = lessTokensThanParts(ls => pathStartingWith(ls.map(s => routeData(Symbol(s)))) serve null)

  }

  object AllDataRouteNoStar extends AllDataPathDataShared {
    override val routeF: List[RoutePart] => Route = l => pathMatching(l) serve null

    def testEqualLengths = check {
      (pathParts: List[String]) =>
        pathMatching(pathParts.map(s => routeData(Symbol(s)))).serve(null).isDefinedAt(ReqRespData(pathParts = pathParts)) must beTrue
    }

    def testUnequalLengths = forAll(differingLists) {
      (lists: (List[String],List[String])) => {
        val (ls1,ls2) = lists
        pathMatching(ls1.map(s => routeData(Symbol(s)))).serve(null).isDefinedAt(ReqRespData(pathParts = ls2)) must beFalse
      }
    }
  }

  def testStringRoutesAlwaysHaveEmptyPathInfo = check {
    (pathParts: List[String], bool: Boolean) => {
      val terms = pathParts.map(routeToken(_))
      val route = if (bool) pathMatching(terms) serve null else pathStartingWith(terms) serve null
      route.apply(ReqRespData(pathParts = pathParts))._2.pathInfo must beEmpty
    }      
  }
  
  object AllStringRoutesWithStar extends AllStringsPathDataShared {
    override val routeF: List[RoutePart] => Route = l => pathStartingWith(l) serve null
    
    def testExactStartingWithMatch = check {
      (pathParts: List[String]) => pathStartingWith(pathParts.map(routeToken(_))).serve(null).isDefinedAt(ReqRespData(pathParts = pathParts)) must beTrue
    }

    def testLeftoverTokensStillMatch = forAll(nonEmptyTokens, nonEmptyTokens) {
      (pathParts: List[String], additional: List[String]) =>
        pathStartingWith(pathParts.map(routeToken(_))).serve(null).isDefinedAt(ReqRespData(pathParts = pathParts ++ additional)) must beTrue
    }

    def testPartDoesntMatch = atleastOnePartDoesntMatch(ls => pathStartingWith(ls.map(routeToken(_))) serve null)

    def testLessTokensThanParts = lessTokensThanParts(ls => pathStartingWith(ls.map(routeToken(_))) serve null)

  }

  object AllStringRoutesNoStar extends AllStringsPathDataShared {

    override val routeF: List[RoutePart] => Route = l => pathMatching(l) serve null

    def testExactMatchingPath = check {
      (pathParts: List[String])  => pathMatching(pathParts.map(routeToken(_))).serve(null).isDefinedAt(ReqRespData(pathParts = pathParts)) must beTrue
    }

    def testPartDoesntMatch = atleastOnePartDoesntMatch(ls => pathMatching(ls.map(routeToken(_))) serve null)

    def testLessTokensThanParts = lessTokensThanParts(ls => pathMatching(ls.map(routeToken(_))) serve null)

    def testMoreTokensThanParts = forAll(nonEmptyTokens, nonEmptyTokens) {
      (pathParts: List[String], additional: List[String]) =>
        pathMatching(pathParts.map(routeToken(_))).serve(null).isDefinedAt(ReqRespData(pathParts = pathParts ++ additional)) must beFalse
    }

  }

  val tok = for(cs <- Gen.listOf1(Gen.alphaChar)) yield cs.mkString
  val nonEmptyTokens = Gen.containerOf1[List, String](tok)

  // generates a nonempty token list, an index to change and a value to change that index to
  val tokensAndChangeIndexAndValue =
    for {
      ls <- nonEmptyTokens
      n <- Gen.choose(0,ls.size - 1)
      s <- tok.suchThat(_ != ls(n))
    } yield (ls, n, s)

  // generates a non empty token list and the number of elements to drop from that list
  val tokensAndDropCount =  for { ls <- nonEmptyTokens; n <- Gen.choose(1,ls.size - 1) } yield (ls, n)

  // generates lists of differing sizes
  val differingLists = for {
    ls1 <- nonEmptyTokens
    ls2 <- nonEmptyTokens
    shouldAdd <- Arbitrary.arbitrary[Boolean]
  } yield {
    val (baseList,changeList) = if (ls1.size > ls2.size) (ls1,ls2) else (ls2,ls1)
    if (shouldAdd) (baseList,baseList ++ changeList) else (baseList,baseList drop changeList.size)
  }

  // generates a non-empty token list and a set of indexes to be intended to be used as data parts
  val tokensAndDataPartIdxs = for {
    ls <- nonEmptyTokens suchThat { _.size > 4 }
    idxs <- {
      val lsSize = ls.toSet.size
      Gen.containerOf1[Set,Int](Gen.choose(0,lsSize - 1)) suchThat { _.size < lsSize - 2 }
    }
  } yield (ls.toSet.toList, idxs)
  
  // generates a non-empty token list, a set of indexes inteded to be data parts an index to change that is not one of the data part indexes
  // and a value to change to
  val tokensDataPartIdxsAndChange = for {
    (ls,idxs) <- tokensAndDataPartIdxs
    n <- Gen.choose(0,ls.size - 1).suchThat(n => !(idxs.contains(n)))
    s <- tok
  } yield (ls,idxs,n,s)

  val tokensDataPartIdxsAndDropCount = for {
    (ls,idxs) <- tokensAndDataPartIdxs
    n <- Gen.choose(1,ls.size-1)
  } yield (ls,idxs,n)

  val buildMixedRouteTerms: (List[String],Set[Int]) => List[RoutePart] =
    (pathParts, dataIdxs) => pathParts.zipWithIndex.map {
      case (s,idx) if dataIdxs.contains(idx) => routeData(Symbol(s))
      case (s,_) => routeToken(s)
    }

  def atleastOnePartDoesntMatch(routeF: List[String] => Route) = forAll(tokensAndChangeIndexAndValue) {
    (data: (List[String],Int,String)) => {
      val (pathParts,changeAt,changeTo) = data
      val changedParts = pathParts.toBuffer
      changedParts.update(changeAt, changeTo)
      routeF(pathParts).isDefinedAt(ReqRespData(pathParts = changedParts.toList)) must beFalse
    }
  }

  def lessTokensThanParts(routeF: List[String] => Route) = forAll(tokensAndDropCount) {
    (data: (List[String],Int)) => {
      val (pathParts, dropCount) = data
      routeF(pathParts).isDefinedAt(ReqRespData(pathParts = pathParts.reverse.drop(dropCount).reverse)) must beFalse
    }

  }

}
