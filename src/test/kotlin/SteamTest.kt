import com.jaoafa.vcspeaker.tools.Steam
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class SteamTest : FunSpec({
    test("If the app ID is valid, the app information is returned.") {
        val app = Steam.getAppDetail("270450")

        app.shouldNotBeNull()
        app.success.shouldBeTrue()

        val data = app.data

        data.shouldNotBeNull()
        data.type shouldBe "game"
        data.name shouldBe "Robot Roller-Derby Disco Dodgeball"
    }
})