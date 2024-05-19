import com.jaoafa.vcspeaker.tools.Steam
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull

class SteamTest : FunSpec({
    test("getAppDetail") {
        val app = Steam.getAppDetail("270450")

        app.shouldNotBeNull()
        app.success.shouldBeTrue()

        val data = app.data

        data.shouldNotBeNull()
        data.type shouldBeEqual "game"
        data.name shouldBeEqual "Robot Roller-Derby Disco Dodgeball"
    }
})