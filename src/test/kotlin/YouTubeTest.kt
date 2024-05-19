
import com.jaoafa.vcspeaker.tools.YouTube
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull

class YouTubeTest : FunSpec({
    test("getVideo") {
        val video = YouTube.getVideo("aEbZ1UXcKhI")

        video.shouldNotBeNull()
        video.title shouldBeEqual "とまち式! わかりやすい! BIGみそか"
        video.authorName shouldBeEqual "jaotan"
    }

    test("getPlaylist") {
        val playlist = YouTube.getPlaylist("PL98tDGwe38KzTLvjPA0FJEqHl2y-_QufX")

        playlist.shouldNotBeNull()
        playlist.title shouldBeEqual "1.mp4"
        playlist.authorName shouldBeEqual "jaotan"
    }
})