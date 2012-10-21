package scalamachine.core.tests
package v3

import org.specs2._
import mock._
import org.mockito.{Matchers => MM}
import scalamachine.core.{ResT, Resource, Util, ContentType, HTTPBody, FixedLengthBody, LazyStreamBody}
import scalamachine.core.Resource._
import scalamachine.core.flow._
import scalamachine.core.v3.WebmachineDecisions
import scalamachine.core.Res._
import org.apache.commons.httpclient.util.DateUtil
import java.util.Date
import scalamachine.core.HTTPHeaders._
import scalamachine.core.HTTPMethods._
import scalamachine.core.ReqRespData._
import scalaz.iteratee.{IterateeT, EnumeratorT}
import scalaz.effect.IO
import IO._
import scalaz.StateT._
import scalaz.syntax.monad._


class WebmachineV3Specs extends Specification with Mockito with SpecsHelper with WebmachineDecisions { def is = 
  "WebMachine V3".title                                                             ^
  """
  The WebMachine Version 3 Flow

  http://wiki.basho.com/images/http-headers-status-v3.png
  """                                                                               ^
                                                                                    p^
  "H7 - If-Match Exists?"                                                           ^
    "if If-Match header exists, response with code 412 is returned"                 ! testH7IfMatchExists ^
    "otherwise, I7 is returned"                                                     ! testH7IfMatchMissing ^
                                                                                    p^
  "H10 - If-Unmodified-Since Exists?"                                               ^
    "if header exists, H11 is returned"                                             ! testIfUnmodifiedSinceExists ^
    "otherwise I12 is returned"                                                     ! testIfUnmodifiedSinceMissing ^
                                                                                    p^
  "H11 - If-Unmodified-Since Valid Date?"                                           ^
    "if date is valid RFC822/1123, H12 is returned"                                 ! testIUMSRFC822Valid ^
    "if date is valid RFC850 (1036), H12 is returned"                               ! testIUMSRFC850Valid ^
    "if date is valid ANSI C Time, H12 is returned"                                 ! testIUMSANSICValid ^
    "otherwise, I12 is returned"                                                    ! testIUMSInvalid ^
                                                                                    p^
  "H12 - Resource Last Mod. Date > If-Unmodified-Since Date"                        ^
    "if resource's last modified > If-Unmodified-Since, code 412 returned"          ! testIUMSLessThanLastMod ^
    "otherwise, I12 returned"                                                       ! testIUMSGreaterThanLastMod ^
                                                                                    p^
  "I4 - Resource Moved Permanently?"                                                ^
    "checks resource not moved permanently returning P3 if its not"                 ^ testIsResourceMovedPermanently(i4,p3) ^
                                                                                    p^p^
  "I7 - PUT?"                                                                       ^
    "if the HTTP Method is PUT, I4 is returned"                                     ! testIsPutTrue ^
    "otherwise K7 is returned"                                                      ! testIsPutFalse ^
                                                                                    p^
  "I12 - If-None-Match Exists?"                                                     ^
    "if header exists, I13 is returned"                                             ! testIfNoneMatchExists ^
    "otherwise, L13 is returned"                                                    ! testIfNoneMatchMissing ^
                                                                                    p^
  "I13 - If-None-Match: *?"                                                         ^
    """if header has value "*", J18 is returned"""                                  ! testIfNoneMatchStar ^
    "otherwise, K13 is returned"                                                    ! testIfNoneMatchNotStar ^
                                                                                    p^
  "J18 - GET or HEAD?"                                                              ^
    "If request method is GET, response with code 304 is returned"                  ! testJ18IsGet ^
    "If request method is HEAD, response with code 304 is returned"                 ! testJ18IsHead ^
    "otherwise, response with code 412 returned"                                    ! testJ18Neither ^
                                                                                    p^
  "K5 - Resource Moved Peramently?"                                                 ^
    "checks resource not moved permanently returning L5 if its not"                 ^ testIsResourceMovedPermanently(k5,l5) ^
                                                                                    p^p^
  "K7 - Resource Previously Existed"                                                ^
    "if resource returns true, K5 is returned"                                      ! testResourceExistedPrevTrue ^
    "if resource returns false, L7 is returned"                                     ! testResourceExistedPrevFalse ^
                                                                                    p^
  "K13 - ETag in If-None-Match?"                                                    ^
    "if resource's etag is in list of etags, J18 is returned"                       ! testIfNoneMatchHasEtag ^
    "otherwise, L13 is returned"                                                    ! testIfNoneMatchMissingEtag ^
                                                                                    p^
  "L5 - Resource Moved Temporarily?"                                                ^
    "if temp. location returned, loc set in header, code 307 returned"              ! testResourceMovedTemporarily ^
    "otherwise, M5 returned"                                                        ! testResourceNotMovedTemporarily ^
                                                                                    p^
  "L7 - POST?"                                                                      ^ testIsPost(l7,m7,404) ^
                                                                                    p^
  "L13 - If-Modified-Since Exists?"                                                 ^
    "If header exists, L14 is returned"                                             ! testIMSExists ^
    "otherwise, M16 is returned"                                                    ! testIMSMissing ^
                                                                                    p^
  "L14 - If-Modified-Since Valid Date?"                                             ^
    "if date is valid, L15 is returned"                                             ! testIMSValid ^
    "if date is not valid, M15 is returned"                                         ! testIMSInvalid ^
                                                                                    p^
  "L15 - If-Modified-Since > Now?"                                                  ^
    "if date is in the future, M16 is returned"                                     ! testIMSInFuture ^
    "if the date is not in the future, L17 is returned"                             ! testIMSNotInFuture ^
                                                                                    p^
  "L17 - If-Modified-Since > Last-Modified"                                         ^
    "if resource has not been modified since the given time, code 304 is returned"  ! testLastModLessThanIMS ^
    "if resource has been modified since given time, M16 is returned"               ! testLastModGreaterThanIMS ^
                                                                                    p^
  "M5 - POST?"                                                                      ^ testIsPost(m5,n5,410) ^
                                                                                    p^
  "M7 - Can POST to missing resource?"                                              ^
    "if resource returns true, N11 is returned"                                     ! testDecisionReturnsDecision(m7,n11, r => r.allowMissingPost returns true.point[r.Result]) ^
    "otherwise, response with code 404 is returned"                                 ! testDecisionHaltsWithCode(m7, 404, r => r.allowMissingPost returns false.point[r.Result]) ^
                                                                                    p^
  "M16 - DELETE?"                                                                   ^
    "if request method is DELETE, M20 returned"                                     ! testDecisionReturnsDecision(m16,m20,r => {}, data = createData(method = DELETE)) ^
    "otherwise, N16 returned"                                                       ! testDecisionReturnsDecision(m16,n16,r => {}, data = createData(method = GET)) ^
                                                                                    p^
  "M20 - Call Resource.deleteResource"                                              ^
    "if true is returned, M20b is returned"                                         ! testDecisionReturnsDecision(m20,m20b,r => r.deleteResource returns true.point[r.Result]) ^
    "if false is returned, response with code 500 is returned"                      ! testDecisionHaltsWithCode(m20, 500, r => r.deleteResource returns false.point[r.Result]) ^
                                                                                    p^
  "M20b - Delete Enacted? (Resource.deleteCompleted)"                               ^
    "if true, O20 is returned"                                                      ! testDecisionReturnsDecision(m20b,o20, r => r.deleteCompleted returns true.point[r.Result]) ^
    "if false, response with code 202 is returned"                                  ! testDecisionHaltsWithCode(m20b, 202, r => r.deleteCompleted returns false.point[r.Result]) ^
                                                                                    p^
  "N5 - Can POST to missing resource?"                                              ^
    "if true, N11 returned"                                                         ! testDecisionReturnsDecision(n5,n11, r => r.allowMissingPost returns true.point[r.Result]) ^
    "otherwise, response with code 410 returned"                                    ! testDecisionHaltsWithCode(n5,410,r => r.allowMissingPost returns false.point[r.Result])  ^
                                                                                    p^
  "N11 - Process Post, Determine Redirect"                                          ^
    "Process Post"                                                                  ^
      "if Resource.postIsCreate returns true"                                       ^
        "Resource.createPath is called"                                             ^
          "if None returned, response with code 500 returned"                       ! testCreatePathNone ^
          "if Some(path) is returned"                                               ^
            "The returned path is set as the dispPath in the ReqRespData"           ! testCreatePathSomeSetsDispPath ^
            "if the location header is not set, the full uri is set as its value"   ! testCreatePathSomeLocationNotSet ^
            "if the location header is set, it is not modified"                     ! testCreatePathSomeLocationAlreadySet ^
            "if request's content-type is not one of those accepted, 415 returned"  ! testN11ContentTypeNotAccepted ^
            "if request's ctype is accepted and corresponding function returns true"^
              "if body is set, it is charsetted then encoded"                       ! testN11ContentTypeAcceptedReturnsTrue ^p^
            "if function corresponing to ctype returns false code 500 returned"     ! testN11ContentTypeAcceptedReturnsFalse ^p^p^
      "if Resource.postIsCreate returns false"                                      ^
        "Resource.processPost is called"                                            ^
          "if true, the body is charsetted then encoded if set"                     ! testProcessPostTrue ^
          "if true and body not set, charsetter and encoder not used"               ! testProcessPostTrueBodyNotSet ^
          "if false, response with code 500 is returned"                            ! testProcessPostFalse ^p^p^p^
    "Determine Redirect"                                                            ^
      "If ReqRespData.doRedirect returns true"                                      ^
        "if Location header is set, response with code 303 returned"                ! testDoRedirect ^
      "If ReqRespData.doRedirect returns false, P11 returned"                       ! testNoRedirect ^
                                                                                    p^p^
  "N16 - POST?"                                                                     ^
    "if request is POST, N11 returned"                                              ! testDecisionReturnsDecision(n16,n11,r => {}, data = createData(method = POST)) ^
    "otherwise, O16 returned"                                                       ! testDecisionReturnsDecision(n16,o16,r => {}, data = createData(method = GET)) ^
                                                                                    p^
  "O14 - Conflict?"                                                                 ^
    "if Resource.isConflict returns true, response w/ code 409 returned"            ! testDecisionHaltsWithCode(o14, 409, r => r.isConflict returns true.point[r.Result]) ^
    "otherwise"                                                                     ^
      "if request's ctype is accepted and corresponding func. returns true"         ^
        "if body is set it is charsetted and encoded, P11 returned"                 ! testO14ContentTypeAcceptedReturnsTrue ^p^
      "if request's ctype is accepted but func. returns false, 500 returned"        ! testO14ContentTypeAcceptedReturnsFalse ^
      "if reques's ctype not accepted 415 returned"                                 ! testO14ContentTypeNotAccepted ^
                                                                                    p^p^
  "O16 - PUT?"                                                                      ^
    "if request is PUT, O14 returned"                                               ! testDecisionReturnsDecision(o16,o14,r => {}, data = createData(method = PUT)) ^
    "otherwise, O18 returned"                                                       ! testDecisionReturnsDecision(o16,o18,r => {}, data = createData(method = GET)) ^
                                                                                    p^
  "O18 - Multiple Representations?"                                                 ^
    "If request is a GET or HEAD request"                                           ^
      "if Resource.generateEtag is some, Etag header is set to value"               ! testO18EtagGenerated ^
      "if Resource.lastModified returns date, string value set in Last-Modified"    ! testO18LastModExists ^
      "if Resource.expires returns a datae, string value set in Expires"            ! testO18ExpiresExists ^
      "otherwise Last-Modified, Expires & Etag not set"                             ! testO18NotGenerated  ^
      "chosen content type function is run"                                         ^
        "result is set in body after being charsetted then encoded"                 ! testO18BodyProductionTest ^
        "lazy streamed bodies are also charset then encoded"                        ! testO18LazyStreamBodyProductionTest ^p^p^
    "If Resource.multipleChoices returns true, response with code 300 returned"     ! testMultipleChoicesTrue ^
    "otherwise response with code 200 returned"                                     ! testMultipleChoicesFalse ^
                                                                                    p^
  "O20 - Response includes an entity?"                                              ^
    "if EmptyBody, response with code 204 returned"                                 ! testDecisionHaltsWithCode(o20,204,r => {}) ^
    "otherwise, O18 returned"                                                       ! testDecisionReturnsDecision(o20,o18,r =>{},data=createData(respBody="1".getBytes)) ^
                                                                                    p^
  "P11 - New Resource?"                                                             ^
    "if location header is set, response with code 201 returned"                    ! testDecisionHaltsWithCode(p11,201,r=>{},data=createData(respHdrs=Map(Location-> "a"))) ^
    "otherwise, O20 returned"                                                       ! testDecisionReturnsDecision(p11,o20,r => {})
                                                                                    end

  // TODO: tests around halt result, error result, empty result, since that logic is no longer in flow runner where test used to be

  def testMultipleChoicesFalse = {
    def stub(r: Resource) {
      val ctypes: r.ContentTypesProvided = (ContentType("text/plain"), (HTTPBody.Empty: HTTPBody).point[r.Result]) :: Nil

      r.generateEtag returns (None: Option[String]).point[r.Result]
      r.lastModified returns (None: Option[Date]).point[r.Result]
      r.expires returns (None: Option[Date]).point[r.Result]
      r.contentTypesProvided returns ctypes.point[r.Result]
      r.charsetsProvided returns (None: CharsetsProvided).point[r.Result]
      r.encodingsProvided returns (None: EncodingsProvided).point[r.Result]
      r.multipleChoices returns false.point[r.Result]
    }
    testDecisionReturnsData(
      o18,
      stub(_),
      data = createData(
        method = GET,
        metadata = Metadata(
          contentType = Some(ContentType("text/plain")),
          chosenEncoding = Some("enc1"),
          chosenCharset = Some("ch1")
        )
      )
    ) { _.statusCode must beEqualTo(200) }
  }

  def testMultipleChoicesTrue = {
    def stub(r: Resource) {
      val ctypes: r.ContentTypesProvided = (ContentType("text/plain"), (HTTPBody.Empty: HTTPBody).point[r.Result]) :: Nil

      r.generateEtag returns (None: Option[String]).point[r.Result]
      r.lastModified returns (None: Option[Date]).point[r.Result]
      r.expires returns (None: Option[Date]).point[r.Result]
      r.contentTypesProvided returns ctypes.point[r.Result]
      r.charsetsProvided returns (None: CharsetsProvided).point[r.Result]
      r.encodingsProvided returns (None: EncodingsProvided).point[r.Result]
      r.multipleChoices returns true.point[r.Result]
    }
    testDecisionHaltsWithCode(
      o18,
      300,
      stub(_),
      data = createData(
        method = GET,
        metadata = Metadata(
          contentType = Some(ContentType("text/plain")),
          chosenEncoding = Some("enc1"),
          chosenCharset = Some("ch1")
        )
      )
    )
  }

  def testO18BodyProductionTest = {
    val setBody: String = "body1"
    val charsetBody: String = "charsetBody"
    val encBody: String = "encbody"

    def stub(r: Resource) {
      val ctypes: r.ContentTypesProvided = (ContentType("text/plain"), (FixedLengthBody(setBody): HTTPBody).point[r.Result]) :: Nil
      val charsets: CharsetsProvided = Some(("ch1", ((_: Array[Byte]) ++ charsetBody.getBytes)) :: Nil)
      val encodings: EncodingsProvided = Some(("enc1", ((_: Array[Byte]) ++ encBody.getBytes)) :: Nil)

      r.generateEtag returns (None: Option[String]).point[r.Result]
      r.lastModified returns (None: Option[Date]).point[r.Result]
      r.expires returns (None: Option[Date]).point[r.Result]
      r.contentTypesProvided returns ctypes.point[r.Result]
      r.charsetsProvided returns charsets.point[r.Result]
      r.encodingsProvided returns encodings.point[r.Result]
      r.multipleChoices returns false.point[r.Result]
    }
    testDecisionResultHasData(
      o18,
      stub(_),
      data = createData(
        metadata = Metadata(
          contentType = Some(ContentType("text/plain")),
          chosenEncoding = Some("enc1"),
          chosenCharset = Some("ch1")
        )
      )
    ) {
      _.responseBody.stringValue must beEqualTo(setBody + charsetBody + encBody)
    }
  }

  def testO18LazyStreamBodyProductionTest = {
    import scalaz.std.list._

    val charsetPrefix: String = "c"
    val encPrefix: String = "e"

    def stub(r: Resource) {
      val bodyParts = List("a".getBytes, "b".getBytes, "c".getBytes).map(b => HTTPBody.ByteChunk(b))
      val body: HTTPBody = LazyStreamBody(IO(EnumeratorT.enumList[HTTPBody.Chunk,IO](bodyParts)))
      val ctypes: r.ContentTypesProvided = (ContentType("text/plain"), body.point[r.Result]) :: Nil
      val charsets: CharsetsProvided = Some(("ch1", (a: Array[Byte]) => charsetPrefix.getBytes ++ a) :: Nil)
      val encodings: EncodingsProvided = Some(("enc1", (a: Array[Byte]) => encPrefix.getBytes ++ a) :: Nil)

      r.generateEtag returns (None: Option[String]).point[r.Result]
      r.lastModified returns (None: Option[Date]).point[r.Result]
      r.expires returns (None: Option[Date]).point[r.Result]
      r.contentTypesProvided returns ctypes.point[r.Result]
      r.charsetsProvided returns charsets.point[r.Result]
      r.encodingsProvided returns encodings.point[r.Result]
      r.multipleChoices returns false.point[r.Result]
    }
    testDecisionResultHasData(
      o18,
      stub(_),
      data = createData(
        metadata = Metadata(
          contentType = Some(ContentType("text/plain")),
          chosenEncoding = Some("enc1"),
          chosenCharset = Some("ch1")
        )
      )
    ) {
      data => {
        val listIO = for {
          e <- data.responseBody.lazyStream
          list <- (IterateeT.consume[HTTPBody.Chunk,IO,List] &= e).run
        } yield list

        val chunks = listIO.unsafePerformIO map {
          case HTTPBody.ByteChunk(bytes) => new String(bytes)
          case _ => ""
        }

        forall(chunks)((s: String) => s must startWith(encPrefix + charsetPrefix))
      }
    }
  }

  def testO18ExpiresExists = {
    val expires = new Date(System.currentTimeMillis)
    def stub(r: Resource) {
      val ctypes: r.ContentTypesProvided = (ContentType("text/plain"), (HTTPBody.Empty: HTTPBody).point[r.Result]) :: Nil

      r.generateEtag returns (None: Option[String]).point[r.Result]
      r.lastModified returns (None: Option[Date]).point[r.Result]
      r.expires returns Option(expires).point[r.Result]
      r.contentTypesProvided returns ctypes.point[r.Result]
      r.charsetsProvided returns (None: CharsetsProvided).point[r.Result]
      r.encodingsProvided returns (None: EncodingsProvided).point[r.Result]
      r.multipleChoices returns false.point[r.Result]
    }
    testDecisionResultHasData(
      o18,
      stub(_),
      data = createData(
        metadata = Metadata(
          contentType = Some(ContentType("text/plain")),
          chosenEncoding = Some("enc1"),
          chosenCharset = Some("ch1")
        )
      )
    ) {
      _.responseHeader(Expires) must beSome.like {
        case date => date must beEqualTo(Util.formatDate(expires))
      }
    }
  }

  def testO18LastModExists = {
    val lastMod = new Date(System.currentTimeMillis)
    def stub(r: Resource) {
      val ctypes: r.ContentTypesProvided = (ContentType("text/plain"), (HTTPBody.Empty: HTTPBody).point[r.Result]) :: Nil

      r.generateEtag returns (None: Option[String]).point[r.Result]
      r.lastModified returns Option(lastMod).point[r.Result]
      r.expires returns (None: Option[Date]).point[r.Result]
      r.contentTypesProvided returns ctypes.point[r.Result]
      r.charsetsProvided returns (None: CharsetsProvided).point[r.Result]
      r.encodingsProvided returns (None: EncodingsProvided).point[r.Result]
      r.multipleChoices returns false.point[r.Result]
      
    }
    testDecisionResultHasData(
      o18,
      stub(_),
      data = createData(
        metadata = Metadata(
          contentType = Some(ContentType("text/plain")),
          chosenEncoding = Some("enc1"),
          chosenCharset = Some("ch1")
        )
      )
    ) {
      _.responseHeader(LastModified) must beSome.like {
        case date => date must beEqualTo(Util.formatDate(lastMod))
      }
    }
  }

  def testO18EtagGenerated = {
    val etag = "etag"
    def stub(r: Resource) {
      val ctypes: r.ContentTypesProvided = (ContentType("text/plain"), (HTTPBody.Empty: HTTPBody).point[r.Result]) :: Nil

      r.generateEtag returns Option(etag).point[r.Result]
      r.lastModified returns (None: Option[Date]).point[r.Result]
      r.expires returns (None: Option[Date]).point[r.Result]
      r.contentTypesProvided returns ctypes.point[r.Result]
      r.charsetsProvided returns (None: CharsetsProvided).point[r.Result]
      r.encodingsProvided returns (None: EncodingsProvided).point[r.Result]
      r.multipleChoices returns false.point[r.Result]
    }
    testDecisionResultHasData(
      o18,
      stub(_),
      data = createData(method = GET)
    ) {
      _.responseHeader(ETag) must beSome.like {
        case e => e must_== etag
      }
    }
  }

  def testO18NotGenerated = {
    def stub(r: Resource) {
      val ctypes: r.ContentTypesProvided = (ContentType("text/plain"), (HTTPBody.Empty: HTTPBody).point[r.Result]) :: Nil

      r.generateEtag returns (None: Option[String]).point[r.Result]
      r.lastModified returns (None: Option[Date]).point[r.Result]
      r.expires returns (None: Option[Date]).point[r.Result]
      r.contentTypesProvided returns ctypes.point[r.Result]
      r.charsetsProvided returns (None: CharsetsProvided).point[r.Result]
      r.encodingsProvided returns (None: EncodingsProvided).point[r.Result]
      r.multipleChoices returns false.point[r.Result]
    }      
    testDecisionResultHasData(
      o18,
      stub(_),
      data = createData(method = HEAD)
    ) {
      d =>
        (d.responseHeader(ETag) must beNone) and
          (d.responseHeader(LastModified) must beNone) and
          (d.responseHeader(Expires) must beNone)
    }
  }

  def testH7IfMatchMissing = {
    testDecisionReturnsDecision(h7,i7,r => {})
  }

  def testH7IfMatchExists = {
    testDecisionHaltsWithCode(h7, 412, r => {}, data = createData(headers = Map(IfMatch -> "*")))
  }

  def testIfUnmodifiedSinceExists = {
    testDecisionReturnsDecision(h10, h11, r => {}, data = createData(headers = Map(IfUnmodifiedSince -> "whocares")))
  }

  def testIfUnmodifiedSinceMissing = {
    testDecisionReturnsDecision(h10,i12,r => {})
  }

  def testIUMSRFC822Valid = {
    testDecisionReturnsDecision(h11,h12,r => {}, data = createData(headers = Map(IfUnmodifiedSince -> "Sun, 06 Nov 1994 08:49:37 GMT")))
  }

  def testIUMSRFC850Valid = {
    testDecisionReturnsDecision(h11,h12,r => {}, data = createData(headers = Map(IfUnmodifiedSince -> "Sunday, 06-Nov-94 08:49:37 GMT")))
  }

  def testIUMSANSICValid = {
    testDecisionReturnsDecision(h11,h12,r => {}, data = createData(headers = Map(IfUnmodifiedSince -> "Sun Nov  6 08:49:37 1994")))
  }

  def testIUMSInvalid = {
    testDecisionReturnsDecision(h11,i12,r => {}, data = createData(headers = Map(IfUnmodifiedSince -> "invalid")))
  }

  def testIUMSLessThanLastMod = {
    val date = DateUtil.parseDate("Sat, 29 Oct 1995 19:43:31 GMT")
    testDecisionHaltsWithCode(h12, 412, r => r.lastModified returns Option(date).point[r.Result], data = createData(headers = Map(IfUnmodifiedSince -> "Sat, 29 Oct 1994 19:43:31 GMT")))
  }

  def testIUMSGreaterThanLastMod = {
    val date = DateUtil.parseDate("Sat, 29 Oct 1993 19:43:31 GMT")
    testDecisionReturnsDecision(h12,i12, r => r.lastModified returns Option(date).point[r.Result], data = createData(headers = Map(IfUnmodifiedSince -> "Sat, 29 Oct 1994 19:43:31 GMT")))
  }

  def testIsPutTrue = {
    testDecisionReturnsDecision(i7,i4, r => {}, data = createData(method = PUT))
  }

  def testIsPutFalse = {
    testDecisionReturnsDecision(i7,k7, r => {})
  }

  def testIfNoneMatchExists = {
    testDecisionReturnsDecision(i12,i13,r => {},data = createData(headers = Map(IfNoneMatch -> "*")))
  }

  def testIfNoneMatchMissing = {
    testDecisionReturnsDecision(i12,l13,r => {})
  }

  def testIsResourceMovedPermanently(toTest: Decision, proceed: Decision) =
    "if resource returns a location where the resource has been moved"            ^
      "response has Location header set to returned value, and status 301"        ! testResourceMovedPermanently(toTest) ^p^
    "otherwise the decision to proceed with is returned"                          ! testResourceNotMovedPermanently(toTest,proceed)


  def testResourceMovedPermanently(toTest: Decision) = {
    val location = "http://somewhere.com"
    def stub(r: Resource) { r.movedPermanently returns Option(location).point[r.Result] }
    testDecisionHaltsWithCode(toTest, 301, stub(_)) and testDecisionReturnsData(toTest,stub(_)) {
      d => d.responseHeader(Location) must beSome.like {
        case loc => loc must beEqualTo(location)
      }
    }
  }

  def testResourceNotMovedPermanently(toTest: Decision, proceed: Decision) = {
    testDecisionReturnsDecision(toTest,proceed,r => r.movedPermanently returns (None: Option[String]).point[r.Result])
  }

  def testIfNoneMatchStar = {
    testDecisionReturnsDecision(i13,j18,r => {},data = createData(headers = Map(IfNoneMatch -> "*")))
  }

  def testIfNoneMatchNotStar = {
    testDecisionReturnsDecision(i13,k13,r => {}, data = createData(headers = Map(IfNoneMatch -> "notstar")))
  }

  def testJ18IsGet = {
    testDecisionHaltsWithCode(j18,304,r => {}, data = createData(method = GET))
  }

  def testJ18IsHead = {
    testDecisionHaltsWithCode(j18,304,r => {}, data = createData(method = HEAD))
  }

  def testJ18Neither = {
    testDecisionHaltsWithCode(j18, 412, r => {}, data = createData(method = POST))
  }

  def testResourceExistedPrevTrue = {
    testDecisionReturnsDecision(k7,k5, r => r.previouslyExisted returns true.point[r.Result])
  }

  def testResourceExistedPrevFalse = {
    testDecisionReturnsDecision(k7,l7,  r => r.previouslyExisted returns false.point[r.Result])
  }

  def testIfNoneMatchHasEtag = {
    testDecisionReturnsDecision(k13,j18, r => r.generateEtag returns Option("1").point[r.Result], data = createData(headers = Map(IfNoneMatch -> "1,2")))
  }

  def testIfNoneMatchMissingEtag = {
    testDecisionReturnsDecision(k13,l13, r => r.generateEtag returns (None: Option[String]).point[r.Result], data = createData(headers = Map(IfNoneMatch -> "1,2")))
  }

  def testResourceMovedTemporarily = {
    val location = "http://abc.com"
    def stub(r: Resource) { r.movedTemporarily returns Option(location).point[r.Result] }
    testDecisionHaltsWithCode(l5, 307, stub(_)) and testDecisionReturnsData(l5,stub(_)) {
      d => d.responseHeader(Location) must beSome.like {
        case loc => loc must beEqualTo(location)
      }
    }
  }

  def testResourceNotMovedTemporarily = {
    testDecisionReturnsDecision(l5,m5, r => r.movedTemporarily returns (None: Option[String]).point[r.Result])
  }

  def testIsPost(toTest: Decision,whenPost:Decision,whenNot:Int) =
    "if request method is POST, " + whenPost.name + "  is returned"                   ! testRequestIsPost(toTest,whenPost) ^
    "if request method is not POST, halts w/ code " + whenNot                         ! testRequestNotPost(toTest,whenNot)


  def testRequestIsPost(toTest: Decision, expected: Decision) = {
    testDecisionReturnsDecision(toTest,expected,r => {},data = createData(method = POST))
  }

  def testRequestNotPost(toTest: Decision, responseCode: Int) = {
    testDecisionHaltsWithCode(toTest,responseCode,r => {},data = createData(method = GET))
  }

  def testIMSMissing = {
    testDecisionReturnsDecision(l13,m16,r => {})
  }

  def testIMSExists = {
    testDecisionReturnsDecision(l13,l14,r => {}, data = createData(headers = Map(IfModifiedSince -> "*")))
  }

  def testIMSValid = {
    testDecisionReturnsDecision(l14,l15,r => {}, data = createData(headers = Map(IfModifiedSince -> "Sun, 06 Nov 1994 08:49:37 GMT")))
  }

  def testIMSInvalid = {
    testDecisionReturnsDecision(l14,m16,r => {}, data = createData(headers = Map(IfModifiedSince -> "invalid")))
  }

  def testIMSInFuture = {
    // hack
    testDecisionReturnsDecision(l15,m16,r => {}, data = createData(headers = Map(IfModifiedSince -> "Sun, 06 Nov 2050 08:49:37 GMT")))
  }

  def testIMSNotInFuture = {
    // hack
    testDecisionReturnsDecision(l15,l17,r => {}, data = createData(headers = Map(IfModifiedSince -> "Sun, 06 Nov 1994 08:49:37 GMT")))
  }

  def testLastModGreaterThanIMS = {
    testDecisionReturnsDecision(
      l17,
      m16,
      r => r.lastModified returns Util.parseDate("Sun, 06 Nov 1995 08:49:37 GMT").point[r.Result],
      data = createData(headers = Map(IfModifiedSince -> "Sun, 06 Nov 1994 08:49:37 GMT"))
    )
  }

  def testLastModLessThanIMS = {
    testDecisionHaltsWithCode(
      l17,
      304,
      r => r.lastModified returns Util.parseDate("Sun, 06 Nov 1993 08:49:37 GMT").point[r.Result],
      data = createData(headers = Map(IfModifiedSince -> "Sun, 06 Nov 1994 08:49:37 GMT"))
    )
  }

  def testDoRedirect = {
    def stub(r: Resource) {
      val setBody = "body1"
      val encodingBody = "body2"
      val charsetBody = "body3"
      val encodings: EncodingsProvided = Some(("enc1", (s: Array[Byte]) => s ++ encodingBody.getBytes) :: ("enc2", identity[Array[Byte]](_)) :: Nil)
      val charsets: EncodingsProvided = Some(("ch1", (s: Array[Byte]) => s ++ charsetBody.getBytes) :: ("ch2", identity[Array[Byte]](_)) :: Nil)
      val bodyR: r.Result[Boolean] = ((r.dataL >=> respBodyL) := setBody).map(_ => true).lift[IO].liftM[ResT] 
      val contentTypesAccepted: r.ContentTypesAccepted =
        (ContentType("text/plain"), bodyR) :: (ContentType("text/html"), false.point[r.Result]) :: Nil

      r.postIsCreate returns true.point[r.Result]
      r.createPath returns Option("a/b").point[r.Result]
      r.contentTypesAccepted returns contentTypesAccepted.point[r.Result]
      r.encodingsProvided returns encodings.point[r.Result]
      r.charsetsProvided returns charsets.point[r.Result]
    }

    testDecisionHaltsWithCode(
      n11,
      303,
      stub(_),
      data = createData(
        metadata = Metadata(chosenCharset = Some("ch1"), chosenEncoding = Some("enc1")),
        headers = Map(ContentTypeHeader -> "text/plain"),
        respHdrs = Map(Location -> "someloc"),
        doRedirect = true
      )
    )
  }

  def testNoRedirect = {
    def stub(r: Resource) {
      val setBody = "body1"
      val encodingBody = "body2"
      val charsetBody = "body3"
      val encodings: EncodingsProvided = Some(("enc1", (s: Array[Byte]) => s ++ encodingBody.getBytes) :: ("enc2", identity[Array[Byte]](_)) :: Nil)
      val charsets: EncodingsProvided = Some(("ch1", (s: Array[Byte]) => s ++ charsetBody.getBytes) :: ("ch2", identity[Array[Byte]](_)) :: Nil)
      val bodyR: r.Result[Boolean] = ((r.dataL >=> respBodyL) := setBody).lift[IO].liftM[ResT].map(_ => true) 
      val contentTypesAccepted: r.ContentTypesAccepted =
        (ContentType("text/plain"), bodyR) :: (ContentType("text/html"), false.point[r.Result]) :: Nil

      r.postIsCreate returns true.point[r.Result]
      r.createPath returns Option("a/b").point[r.Result]
      r.contentTypesAccepted returns contentTypesAccepted.point[r.Result]
      r.encodingsProvided returns encodings.point[r.Result]
      r.charsetsProvided returns charsets.point[r.Result]
    }
    testDecisionReturnsDecision(
      n11,
      p11,
      stub(_),
      data = createData(metadata = Metadata(chosenCharset = Some("ch1"), chosenEncoding = Some("enc1")), headers = Map(ContentTypeHeader -> "text/plain"))
    )
  }

  def testN11ContentTypeNotAccepted = {
    def stub(r: Resource) {
      val contentTypesAccepted: r.ContentTypesAccepted =
        (ContentType("text/html"), false.point[r.Result]) :: Nil

      r.postIsCreate returns true.point[r.Result]
      r.createPath returns Option("a/b").point[r.Result]
      r.contentTypesAccepted returns contentTypesAccepted.point[r.Result]

    }
    testDecisionHaltsWithCode(
      n11,
      415,
      stub(_),
      data = createData(headers = Map(ContentTypeHeader -> "text/html2"))
    )

  }

  def testN11ContentTypeAcceptedReturnsFalse = {
    def stub(r: Resource) {
      val contentTypesAccepted: r.ContentTypesAccepted =
        (ContentType("text/html"), false.point[r.Result]) ::Nil

      r.postIsCreate returns true.point[r.Result]
      r.createPath returns Option("a/b").point[r.Result]
      r.contentTypesAccepted returns contentTypesAccepted.point[r.Result]
    }
    testDecisionHaltsWithCode(
      n11,
      500,
      stub(_),
      data = createData(headers = Map(ContentTypeHeader -> "text/html"))
    )

  }

  def testN11ContentTypeAcceptedReturnsTrue = {
    val setBody = "body1"
    val encodingBody = "body2"
    val charsetBody = "body3"

    def stub(r: Resource) {
      val encodings: EncodingsProvided = Some(("enc1", (s: Array[Byte]) => s ++ encodingBody.getBytes) :: ("enc2", identity[Array[Byte]](_)) :: Nil)
      val charsets: EncodingsProvided = Some(("ch1", (s: Array[Byte]) => s ++ charsetBody.getBytes) :: ("ch2", identity[Array[Byte]](_)) :: Nil)
      val bodyR: r.Result[Boolean] = ((r.dataL >=> respBodyL) := setBody).lift[IO].liftM[ResT].map(_ => true) 
      val contentTypesAccepted: r.ContentTypesAccepted =
        (ContentType("text/plain"), bodyR) :: (ContentType("text/html"), false.point[r.Result]) :: Nil

      r.postIsCreate returns true.point[r.Result]
      r.createPath returns Option("a/b").point[r.Result]
      r.contentTypesAccepted returns contentTypesAccepted.point[r.Result]
      r.encodingsProvided returns encodings.point[r.Result]
      r.charsetsProvided returns charsets.point[r.Result]
    }
    testDecisionResultHasData(
      n11,
      stub(_),
      data = createData(metadata = Metadata(chosenCharset = Some("ch1"), chosenEncoding = Some("enc1")), headers = Map(ContentTypeHeader-> "text/plain"))
    ) {
      _.responseBody.stringValue must beEqualTo(setBody + charsetBody + encodingBody)
    }
  }

  def testCreatePathSomeLocationAlreadySet = {
    val existing = "somelocation"
    testDecisionResultHasData(
      n11,
      r => {
        r.postIsCreate returns true.point[r.Result]
        r.createPath returns Option("a/b").point[r.Result]
        r.contentTypesAccepted returns (Nil: r.ContentTypesAccepted).point[r.Result]
      },
      data = createData(respHdrs = Map(Location -> existing))
    ) {
      _.responseHeader(Location) must beSome.like {
        case loc => loc must beEqualTo(existing)
      }
    }
  }

  def testCreatePathSomeLocationNotSet = {
    val baseUri = "http://example.com"
    val createPath = "a/v"
    testDecisionResultHasData(
      n11,
      r => {
        r.postIsCreate returns true.point[r.Result]
        r.createPath returns Option(createPath).point[r.Result]
        r.contentTypesAccepted returns (Nil: r.ContentTypesAccepted).point[r.Result]
      },
      data = createData(baseUri = baseUri, pathParts = "b" :: Nil)
    ) {
      _.responseHeader(Location) must beSome.like {
        case loc => loc must beEqualTo("http://example.com/b/a/v")
      }
    }
  }

  def testCreatePathNone = {
    testDecisionHaltsWithCode(
      n11,
      500,
      r => {
        r.postIsCreate returns true.point[r.Result]
        r.createPath returns (None: Option[String]).point[r.Result]
      }
    )
  }

  def testCreatePathSomeSetsDispPath = {
    val createPath = "a/b"
    testDecisionResultHasData(
      n11,
      r => {
        r.postIsCreate returns true.point[r.Result]
        r.createPath returns Option(createPath).point[r.Result]
        r.contentTypesAccepted returns (Nil: r.ContentTypesAccepted).point[r.Result]
      }
    ) { _.dispPath must beEqualTo(createPath) }
  }

  def testProcessPostTrue = {
    val processPostBody = "body1"
    val encodingBody = "body2"
    val charsetBody = "body3"
    val encodings: EncodingsProvided = Some(("enc1", (s: Array[Byte]) => s ++ encodingBody.getBytes) :: ("enc2", identity[Array[Byte]](_)) :: Nil)
    val charsets: EncodingsProvided = Some(("ch1", (s: Array[Byte]) => s ++ charsetBody.getBytes) :: ("ch2", identity[Array[Byte]](_)) :: Nil)
    testDecisionResultHasData(
      n11,
      r => {
        r.postIsCreate returns false.point[r.Result]
        r.processPost returns ((r.dataL >=> respBodyL) := processPostBody).lift[IO].liftM[ResT].map(_ => true)
        r.encodingsProvided returns encodings.point[r.Result]
        r.charsetsProvided returns charsets.point[r.Result]
      },
      data = createData(metadata = Metadata(chosenCharset = Some("ch1"), chosenEncoding = Some("enc1")))
    ) { newData =>
      newData.responseBody.stringValue must beEqualTo(processPostBody + charsetBody + encodingBody)
    }
  }

  def testProcessPostTrueBodyNotSet = {
    val encodingBody = "body2"
    val charsetBody = "body3"
    val encodings: EncodingsProvided = Some(("enc1", (s: Array[Byte]) => s ++ encodingBody.getBytes) :: ("enc2", identity[Array[Byte]](_)) :: Nil)
    val charsets: EncodingsProvided = Some(("ch1", (s: Array[Byte]) => s ++ charsetBody.getBytes) :: ("ch2", identity[Array[Byte]](_)) :: Nil)
    testDecisionResultHasData(
      n11,
      r => {
        r.postIsCreate returns false.point[r.Result]
        r.processPost returns true.point[r.Result]
        r.encodingsProvided returns encodings.point[r.Result]
        r.charsetsProvided returns charsets.point[r.Result]
      },
      data = createData(metadata = Metadata(chosenCharset = Some("ch1"), chosenEncoding = Some("enc1")))
    ) { _.responseBody must beEqualTo(HTTPBody.Empty) }
  }

  def testProcessPostFalse = {
    val encodingBody = "body2"
    val charsetBody = "body3"
    val encodings: EncodingsProvided = Some(("enc1", (s: Array[Byte]) => s ++ encodingBody.getBytes) :: ("enc2", identity[Array[Byte]](_)) :: Nil)
    val charsets: EncodingsProvided = Some(("ch1", (s: Array[Byte]) => s ++ charsetBody.getBytes) :: ("ch2", identity[Array[Byte]](_)) :: Nil)
    testDecisionHaltsWithCode(
      n11,
      500,
      r => {
        r.postIsCreate returns false.point[r.Result]
        r.processPost returns false.point[r.Result]
      }
    )
  }

  def testO14ContentTypeAcceptedReturnsTrue = {
    val setBody = "body1"
    val encodingBody = "body2"
    val charsetBody = "body3"
    
    def stub(r: Resource) {
      val encodings: EncodingsProvided = Some(("enc1", (s: Array[Byte]) => s ++ encodingBody.getBytes) :: ("enc2", identity[Array[Byte]](_)) :: Nil)
      val charsets: EncodingsProvided = Some(("ch1", (s: Array[Byte]) => s ++ charsetBody.getBytes) :: ("ch2", identity[Array[Byte]](_)) :: Nil)
      val bodyR: r.Result[Boolean] = ((r.dataL >=> respBodyL) := setBody).lift[IO].liftM[ResT].map(_ => true)
      val contentTypesAccepted: r.ContentTypesAccepted =
        (ContentType("text/plain"), bodyR) :: (ContentType("text/html"), false.point[r.Result]) :: Nil

      r.isConflict returns false.point[r.Result]
      r.contentTypesAccepted returns contentTypesAccepted.point[r.Result]
      r.encodingsProvided returns encodings.point[r.Result]
      r.charsetsProvided returns charsets.point[r.Result]

    }
    testDecisionReturnsDecisionAndData(
      o14,
      p11,
      stub(_),
      data = createData(metadata = Metadata(chosenCharset = Some("ch1"), chosenEncoding = Some("enc1")), headers = Map(ContentTypeHeader-> "text/plain"))
    ) {
      _.responseBody.stringValue must beEqualTo(setBody + charsetBody + encodingBody)
    }
  }

  def testO14ContentTypeAcceptedReturnsFalse = {    
    def stub(r: Resource) {
      val contentTypesAccepted: r.ContentTypesAccepted =
        (ContentType("text/html"), false.point[r.Result]) :: Nil

      r.isConflict returns false.point[r.Result]
      r.contentTypesAccepted returns contentTypesAccepted.point[r.Result]
    }
    testDecisionHaltsWithCode(
      o14,
      500,
      stub(_),
      data = createData(headers = Map(ContentTypeHeader -> "text/html"))
    )

  }

  def testO14ContentTypeNotAccepted = {
    testDecisionHaltsWithCode(
      o14,
      415,
      r => {
        r.isConflict returns false.point[r.Result]
        r.contentTypesAccepted returns (Nil: r.ContentTypesAccepted).point[r.Result]
      },
      data = createData(headers = Map(ContentTypeHeader -> "text/html"))
    ) 

  }

}
