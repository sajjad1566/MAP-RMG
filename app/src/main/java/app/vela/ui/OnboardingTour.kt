package app.vela.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.vela.R
import app.vela.ui.icons.SymOutlined
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The three promo pages a brand-new install sees before anything else (first launch only: the
 * completion flag is [Onboarding.tourDone]). They introduce what the app is, in the order the
 * work was asked for: discover the world, smart navigation, online and offline.
 *
 * Shape of the screen, which is deliberately the same on all three pages so the only thing that
 * moves is the artwork: brand gradient backdrop, a skip pill in the corner, one hero drawing per
 * page, the copy, three feature lines, a segmented progress bar and one primary button.
 *
 * Rules honored here:
 *  - no audio, no network, no permission: a page is Compose drawing only, so a cold launch stays
 *    cheap and nothing is asked for before the privacy pages that follow;
 *  - the pages CANNOT trap anybody: skip is on every page, Back leaves on the first page, and each
 *    page moves on by itself after about four seconds;
 *  - completing or skipping writes the flag once, so this never appears again.
 *
 * D-pad (docs/dpad.md): `dpadClickable` for every button so OK activates them, `dpadHighlight`
 * rings for focus, the primary button is auto-focused on a D-pad-first device, and LEFT/RIGHT
 * move between pages on a keypad only device (the touch path uses the buttons).
 */
@Composable
fun OnboardingTour(onDone: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // One animation drives both the fill of the current segment and the page change, so the bar
    // never lies about the four seconds: when it reaches the end, the page turns. The last page
    // fills quickly and then waits for the button (ابدأ الآن / Start now), which is the user's
    // decision to make, not the timer's.
    val fill = remember { Animatable(0f) }
    LaunchedEffect(page) {
        fill.snapTo(0f)
        fill.animateTo(1f, tween(if (page == LAST_PAGE) 900 else PAGE_MS, easing = LinearEasing))
        if (page < LAST_PAGE) page += 1
    }
    BackHandler(enabled = page > 0) { page -= 1 }

    TourBackdrop {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 22.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                BrandMark(34.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                Spacer(Modifier.weight(1f))
                PillButton(
                    label = stringResource(R.string.tour_skip),
                    filled = false,
                    shape = CircleShape,
                    onClick = onDone,
                )
            }

            val minH = LocalConfiguration.current.screenHeightDp.dp
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = minH - 190.dp),
            ) {
                Spacer(Modifier.height(14.dp))
                AnimatedContent(
                    targetState = page,
                    transitionSpec = {
                        val forward = targetState > initialState
                        // The outgoing page slides towards the edge it came from, the incoming one
                        // follows the reading direction, so the same code reads correctly in Arabic
                        // (RTL) and in the Latin locales.
                        val sign = if (forward == !rtl) 1 else -1
                        (slideInHorizontally(tween(420)) { it * sign * 2 / 3 } + fadeIn(tween(300)))
                            .togetherWith(slideOutHorizontally(tween(420)) { -it * sign * 2 / 3 } + fadeOut(tween(240)))
                    },
                    label = "tour-page",
                ) { index ->
                    PageCopy(
                        page = index,
                        fill = if (index == page) fill.value else 0f,
                        nextLabel = if (index == LAST_PAGE) stringResource(R.string.tour_start) else stringResource(R.string.tour_next),
                        onNext = { if (index == LAST_PAGE) onDone() else page = index + 1 },
                        onKeyMove = { step -> page = (index + step).coerceIn(0, LAST_PAGE) },
                    )
                }
            }

            SegmentedProgress(page = page, fill = fill.value)
            Spacer(Modifier.height(10.dp))
        }
    }
}

private const val LAST_PAGE = 2
private const val PAGE_MS = 4000

/** The page copy: hero drawing, title, body, three feature lines and the primary button. */
@Composable
private fun PageCopy(
    page: Int,
    fill: Float,
    nextLabel: String,
    onNext: () -> Unit,
    onKeyMove: (Int) -> Unit,
) {
    // Entrance animation, restarted for each page: the artwork and the copy rise and fade in, and
    // the three feature lines follow one after another instead of appearing as a block.
    val appear = remember { Animatable(0f) }
    LaunchedEffect(page) {
        appear.snapTo(0f)
        appear.animateTo(1f, tween(560, easing = FastOutSlowInEasing))
    }
    val heroH = (LocalConfiguration.current.screenHeightDp.dp * 0.30f).coerceIn(140.dp, 260.dp)
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(heroH)
                .graphicsAlpha(appear.value),
            contentAlignment = Alignment.Center,
        ) { TourHero(page, fill) }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(pageTitle(page)),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth().graphicsAlpha(appear.value),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(pageBody(page)),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.86f),
            modifier = Modifier.fillMaxWidth().graphicsAlpha(appear.value * 0.95f, lift = 6f),
        )
        Spacer(Modifier.height(16.dp))
        val icons = pageIcons(page)
        val lines = pageLines(page)
        for (i in 0 until 3) {
            val p = ((appear.value - 0.16f * (i + 1)) / 0.52f).coerceIn(0f, 1f)
            FeatureLine(icons[i], stringResource(lines[i]), Modifier.graphicsAlpha(p, lift = 10f * (1f - p)))
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            label = nextLabel,
            gold = page == LAST_PAGE,
            modifier = Modifier.graphicsAlpha(((appear.value - 0.5f) / 0.5f).coerceIn(0f, 1f), lift = 8f),
            onClick = onNext,
            onKeyMove = onKeyMove,
        )
        Spacer(Modifier.height(6.dp))
    }
}

private fun pageTitle(page: Int) = when (page) {
    0 -> R.string.tour_slide1_title
    1 -> R.string.tour_slide2_title
    else -> R.string.tour_slide3_title
}

private fun pageBody(page: Int) = when (page) {
    0 -> R.string.tour_slide1_body
    1 -> R.string.tour_slide2_body
    else -> R.string.tour_slide3_body
}

private fun pageLines(page: Int) = when (page) {
    0 -> intArrayOf(R.string.tour_slide1_p1, R.string.tour_slide1_p2, R.string.tour_slide1_p3)
    1 -> intArrayOf(R.string.tour_slide2_p1, R.string.tour_slide2_p2, R.string.tour_slide2_p3)
    else -> intArrayOf(R.string.tour_slide3_p1, R.string.tour_slide3_p2, R.string.tour_slide3_p3)
}

private fun pageIcons(page: Int): List<ImageVector> = when (page) {
    0 -> listOf(SymOutlined.Map, SymOutlined.Search, SymOutlined.Attractions)
    1 -> listOf(SymOutlined.AltRoute, SymOutlined.Navigation, SymOutlined.Bookmark)
    else -> listOf(SymOutlined.CloudDownload, SymOutlined.Directions, SymOutlined.Shield)
}

/** Fade (and optionally lift) a block in, used by the staggered entrance above. */
private fun Modifier.graphicsAlpha(alpha: Float, lift: Float = 0f): Modifier =
    graphicsLayer(alpha = alpha.coerceIn(0f, 1f), translationY = lift)

/** One feature line: a small glass badge with the glyph, then the label. */
@Composable
private fun FeatureLine(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Color.White.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.95f),
        )
    }
}

/** The segmented bar: three slim segments, the current one filling as its page's seconds pass. */
@Composable
private fun SegmentedProgress(page: Int, fill: Float) {
    val total = stringResource(R.string.tour_step, page + 1, LAST_PAGE + 1)
    Row(
        Modifier
            .fillMaxWidth()
            .semantics { contentDescription = total },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        for (i in 0..LAST_PAGE) {
            val active = i < page
            val fraction = when {
                active -> 1f
                i == page -> fill
                else -> 0f
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.20f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(GoldAccent),
                )
            }
        }
    }
}

/** The primary button: a filled white pill with a royal-blue label, or the gold pill on the last
 *  page, which is the one that starts the app. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PrimaryButton(
    label: String,
    modifier: Modifier = Modifier,
    gold: Boolean = false,
    onClick: () -> Unit,
    onKeyMove: ((Int) -> Unit)? = null,
) {
    val shape = RoundedCornerShape(19.dp)
    val dpadFirst = rememberDpadFirstDevice()
    val requester = remember { FocusRequester() }
    // The copy column scrolls on a short screen or a landscape TV, so a button that takes focus
    // (or is focused for the user on a D-pad first device) asks the scroller to show it. Without
    // this the user could be holding a focused button that is below the fold.
    val scrollReq = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(dpadFirst) {
        if (!dpadFirst) return@LaunchedEffect
        repeat(40) {
            if (runCatching { requester.requestFocus() }.isSuccess) {
                runCatching { scrollReq.bringIntoView() }
                return@LaunchedEffect
            }
            delay(50)
        }
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .focusRequester(requester)
            .dpadHighlight(shape)
            .clip(shape)
            .background(if (gold) GoldAccent else Color.White)
            .dpadClickable(onClick = onClick)
            .then(pageKeys(onKeyMove))
            .bringIntoViewRequester(scrollReq)
            .onFocusChanged { focus ->
                if (focus.isFocused) scope.launch {
                    delay(80)
                    runCatching { scrollReq.bringIntoView() }
                }
            }
            .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (gold) BrandDarkBlue else BrandRoyalBlue,
        )
    }
}

/** The quiet button: a translucent pill, used for Skip all. */
@Composable
private fun PillButton(
    label: String,
    filled: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .dpadHighlight(shape)
            .clip(shape)
            .background(Color.White.copy(alpha = if (filled) 0.92f else 0.16f))
            .dpadClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (filled) BrandRoyalBlue else Color.White,
        )
    }
}

/**
 * LEFT/RIGHT move between pages on a keypad-only device. Which physical direction is "forward"
 * depends on the reading direction, so this asks Compose rather than assuming.
 */
@Composable
private fun pageKeys(onMove: ((Int) -> Unit)?): Modifier {
    if (onMove == null) return Modifier
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    return Modifier.onKeyEvent { ev ->
        if (ev.type != KeyEventType.KeyUp) return@onKeyEvent false
        when (ev.key) {
            Key.DirectionLeft -> onMove(if (rtl) 1 else -1)
            Key.DirectionRight -> onMove(if (rtl) -1 else 1)
            else -> return@onKeyEvent false
        }
        true
    }
}

/** The full-screen brand backdrop: dark blue at the top, royal blue towards the bottom, with two
 *  soft glows so the flat gradient does not read as a plain fill. */
@Composable
private fun TourBackdrop(content: @Composable () -> Unit) {
    val sweep = rememberInfiniteTransition(label = "backdrop")
    val drift by sweep.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift",
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(BrandDarkBlue, androidx.compose.ui.graphics.Color(0xFF12306E), BrandRoyalBlue),
                ),
            ),
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(GoldAccent.copy(alpha = 0.20f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width * (0.18f + 0.10f * drift), size.height * 0.22f),
                    radius = r * 0.75f,
                ),
                radius = r * 0.75f,
                center = androidx.compose.ui.geometry.Offset(size.width * (0.18f + 0.10f * drift), size.height * 0.22f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width * (0.92f - 0.12f * drift), size.height * 0.70f),
                    radius = r * 0.85f,
                ),
                radius = r * 0.85f,
                center = androidx.compose.ui.geometry.Offset(size.width * (0.92f - 0.12f * drift), size.height * 0.70f),
            )
        }
        content()
    }
}

/** The app mark, drawn the way the launcher draws it: brand gradient behind the official art. */
@Composable
private fun BrandMark(size: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(percent = 27))
            .background(BrandRoyalBlue),
    ) {
        Image(
            painter = androidx.compose.ui.res.painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.requiredSize(size),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Hero artwork. One drawing per page, all primitives (no bitmaps, no network), animated by two
// slow phases so the page never looks frozen without ever asking the CPU for much.
// ---------------------------------------------------------------------------------------------

@Composable
private fun TourHero(page: Int, fill: Float) {
    val cycle = rememberInfiniteTransition(label = "hero")
    val spin by cycle.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(11000, easing = LinearEasing)),
        label = "spin",
    )
    val pulse by cycle.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        val unit = size.minDimension
        when (page) {
            0 -> drawGlobe(unit, spin, pulse)
            1 -> drawRoute(unit, spin, pulse)
            else -> drawOffline(unit, pulse, fill)
        }
    }
}

private val GoldAccent = Color(0xFFD4AF37)
private val BrandRoyalBlue = Color(0xFF2457D6)
private val BrandDarkBlue = Color(0xFF0B1C3D)
private val RouteBlue = Color(0xFF4E8BFF)

/** Page 1: a wireframe globe with a gold pin, the pin marking "you are here". */
private fun DrawScope.drawGlobe(unit: Float, spin: Float, pulse: Float) {
    val c = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
    val r = unit * 0.36f
    // Soft halo, then the body of the globe.
    drawCircle(
        brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Transparent), center = c, radius = r * 1.5f),
        radius = r * 1.5f,
        center = c,
    )
    drawCircle(
        brush = Brush.radialGradient(listOf(Color(0xFF3F7BEA), Color(0xFF1B3E8F)), center = c, radius = r),
        radius = r,
        center = c,
    )
    val wire = Color.White.copy(alpha = 0.30f)
    val stroke = Stroke(width = unit * 0.006f, cap = StrokeCap.Round)
    // Longitudes: the outer two stay put, the middle one breathes to suggest rotation.
    val swing = 0.55f + 0.45f * kotlin.math.abs(kotlin.math.sin(spin * Math.PI.toFloat()))
    for (f in floatArrayOf(1f, 0.62f, 0.30f * swing)) {
        drawOval(
            color = wire,
            topLeft = androidx.compose.ui.geometry.Offset(c.x - r * f, c.y - r),
            size = androidx.compose.ui.geometry.Size(r * f * 2f, r * 2f),
            style = stroke,
        )
    }
    // Latitudes: three rings, the middle one widest.
    for (f in floatArrayOf(0.40f, 0.74f, 1f)) {
        drawOval(
            color = wire,
            topLeft = androidx.compose.ui.geometry.Offset(c.x - r, c.y - r * f),
            size = androidx.compose.ui.geometry.Size(r * 2f, r * f * 2f),
            style = stroke,
        )
    }
    drawCircle(color = GoldAccent.copy(alpha = 0.55f), radius = r, center = c, style = Stroke(width = unit * 0.010f))
    // Continent hint: two soft blobs so the globe is not an empty grid.
    drawOval(
        color = Color.White.copy(alpha = 0.16f),
        topLeft = androidx.compose.ui.geometry.Offset(c.x - r * 0.86f, c.y - r * 0.62f),
        size = androidx.compose.ui.geometry.Size(r * 0.78f, r * 0.66f),
    )
    drawOval(
        color = Color.White.copy(alpha = 0.13f),
        topLeft = androidx.compose.ui.geometry.Offset(c.x + r * 0.10f, c.y + r * 0.06f),
        size = androidx.compose.ui.geometry.Size(r * 0.72f, r * 0.86f),
    )
    // The pin, with expanding rings underneath it.
    val pinY = c.y - r * 0.10f
    for (i in 0..1) {
        val t = ((pulse + i * 0.5f) % 1f)
        drawCircle(
            color = GoldAccent.copy(alpha = 0.34f * (1f - t)),
            radius = r * (0.34f + 0.85f * t),
            center = androidx.compose.ui.geometry.Offset(c.x, pinY + r * 0.10f),
            style = Stroke(width = unit * 0.007f),
        )
    }
    val head = r * 0.30f
    val tip = androidx.compose.ui.geometry.Offset(c.x, pinY + r * 0.72f)
    val pin = Path().apply {
        moveTo(c.x - head, pinY)
        cubicTo(c.x - head, pinY - head * 2.2f, c.x + head, pinY - head * 2.2f, c.x + head, pinY)
        quadraticBezierTo(c.x + head * 0.4f, pinY + r * 0.34f, tip.x, tip.y)
        quadraticBezierTo(c.x - head * 0.4f, pinY + r * 0.34f, c.x - head, pinY)
        close()
    }
    drawPath(pin, brush = Brush.linearGradient(
        listOf(Color(0xFFF3DE9A), GoldAccent),
        start = androidx.compose.ui.geometry.Offset(c.x - head, pinY - head * 2f),
        end = androidx.compose.ui.geometry.Offset(c.x + head, pinY + r * 0.7f),
    ))
    drawCircle(color = BrandRoyalBlue, radius = head * 0.44f, center = androidx.compose.ui.geometry.Offset(c.x, pinY - head * 0.22f))
    drawCircle(color = Color.White.copy(alpha = 0.85f), radius = head * 0.16f, center = androidx.compose.ui.geometry.Offset(c.x, pinY - head * 0.22f))
}

/** Page 2: the blue route with the navigation puck running along it. */
private fun DrawScope.drawRoute(unit: Float, phase: Float, pulse: Float) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.08f, h * 0.86f)
        cubicTo(w * 0.34f, h * 0.80f, w * 0.30f, h * 0.44f, w * 0.52f, h * 0.40f)
        cubicTo(w * 0.74f, h * 0.36f, w * 0.72f, h * 0.16f, w * 0.94f, h * 0.14f)
    }
    // Casing, then the route itself, then the part already driven (brighter, the way the app
    // paints the road behind you and the road ahead differently).
    drawPath(path, Color.White.copy(alpha = 0.85f), style = Stroke(width = unit * 0.052f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, RouteBlue, style = Stroke(width = unit * 0.036f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    val measure = PathMeasure().apply { setPath(path, false) }
    val length = measure.length
    val distanceAlong = length * (0.15f + 0.75f * ((phase * 1.6f) % 1f))
    val done = Path()
    measure.getSegment(0f, distanceAlong, done, true)
    drawPath(done, Color(0xFF9DC0FF), style = Stroke(width = unit * 0.036f, cap = StrokeCap.Round))
    // Origin and destination.
    drawCircle(Color.White, radius = unit * 0.022f, center = androidx.compose.ui.geometry.Offset(w * 0.08f, h * 0.86f))
    val dest = androidx.compose.ui.geometry.Offset(w * 0.94f, h * 0.14f)
    drawCircle(GoldAccent.copy(alpha = 0.30f * pulse + 0.35f), radius = unit * 0.055f, center = dest)
    drawCircle(GoldAccent, radius = unit * 0.026f, center = dest)
    // The puck: a white disc with a blue point, turned to the road it is on.
    val at = measure.getPosition(distanceAlong)
    val tangent = measure.getTangent(distanceAlong)
    val angle = kotlin.math.atan2(tangent.y, tangent.x) * 180f / Math.PI.toFloat()
    withTransform({ rotate(degrees = angle + 90f, pivot = at) }) {
        val tip = androidx.compose.ui.geometry.Offset(at.x, at.y - unit * 0.075f)
        val left = androidx.compose.ui.geometry.Offset(at.x - unit * 0.055f, at.y + unit * 0.055f)
        val right = androidx.compose.ui.geometry.Offset(at.x + unit * 0.055f, at.y + unit * 0.055f)
        val chevron = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(left.x, left.y)
            lineTo(at.x, at.y + unit * 0.018f)
            lineTo(right.x, right.y)
            close()
        }
        drawCircle(Color.White, radius = unit * 0.070f, center = at)
        drawPath(chevron, BrandRoyalBlue)
    }
}

/** Page 3: a map area being downloaded, then a gold check. */
private fun DrawScope.drawOffline(unit: Float, pulse: Float, fill: Float) {
    val cardW = size.width * 0.62f
    val cardH = size.height * 0.66f
    val left = size.width / 2f - cardW / 2f
    val top = size.height / 2f - cardH / 2f
    val corner = androidx.compose.ui.geometry.CornerRadius(unit * 0.055f)
    // The tile card, with the seam lines a map area is cut into.
    drawRoundRect(
        color = Color.White.copy(alpha = 0.13f),
        topLeft = androidx.compose.ui.geometry.Offset(left, top),
        size = androidx.compose.ui.geometry.Size(cardW, cardH),
        cornerRadius = corner,
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.34f),
        topLeft = androidx.compose.ui.geometry.Offset(left, top),
        size = androidx.compose.ui.geometry.Size(cardW, cardH),
        cornerRadius = corner,
        style = Stroke(width = unit * 0.008f),
    )
    for (i in 1..2) {
        val x = left + cardW * i / 3f
        val y = top + cardH * i / 3f
        drawLine(Color.White.copy(alpha = 0.20f), androidx.compose.ui.geometry.Offset(x, top), androidx.compose.ui.geometry.Offset(x, top + cardH), strokeWidth = unit * 0.005f)
        drawLine(Color.White.copy(alpha = 0.20f), androidx.compose.ui.geometry.Offset(left, y), androidx.compose.ui.geometry.Offset(left + cardW, y), strokeWidth = unit * 0.005f)
    }
    // The cloud, drawn over the top of the card.
    val cloudC = androidx.compose.ui.geometry.Offset(size.width / 2f, top + cardH * 0.24f)
    val ink = Color.White.copy(alpha = 0.94f)
    drawCircle(ink, radius = unit * 0.052f, center = androidx.compose.ui.geometry.Offset(cloudC.x - unit * 0.040f, cloudC.y))
    drawCircle(ink, radius = unit * 0.066f, center = androidx.compose.ui.geometry.Offset(cloudC.x + unit * 0.012f, cloudC.y - unit * 0.016f))
    drawCircle(ink, radius = unit * 0.046f, center = androidx.compose.ui.geometry.Offset(cloudC.x + unit * 0.062f, cloudC.y + unit * 0.010f))
    drawRoundRect(
        color = ink,
        topLeft = androidx.compose.ui.geometry.Offset(cloudC.x - unit * 0.052f, cloudC.y + unit * 0.030f),
        size = androidx.compose.ui.geometry.Size(unit * 0.128f, unit * 0.030f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(unit * 0.015f),
    )
    // Download arrow under the cloud, the same motion the download cards use.
    val arrowTop = cloudC.y + unit * 0.10f
    val arrowBottom = arrowTop + unit * 0.115f + unit * 0.012f * pulse
    drawLine(
        BrandRoyalBlue,
        androidx.compose.ui.geometry.Offset(cloudC.x, arrowTop),
        androidx.compose.ui.geometry.Offset(cloudC.x, arrowBottom),
        strokeWidth = unit * 0.020f,
        cap = StrokeCap.Round,
    )
    val head = Path().apply {
        moveTo(cloudC.x - unit * 0.042f, arrowBottom - unit * 0.030f)
        lineTo(cloudC.x + unit * 0.042f, arrowBottom - unit * 0.030f)
        lineTo(cloudC.x, arrowBottom + unit * 0.026f)
        close()
    }
    drawPath(head, BrandRoyalBlue)
    // Progress ring, then the gold check once the area is in.
    val ringC = androidx.compose.ui.geometry.Offset(size.width / 2f, top + cardH * 0.72f)
    val ringR = unit * 0.115f
    drawCircle(Color.White.copy(alpha = 0.22f), radius = ringR, center = ringC, style = Stroke(width = unit * 0.020f))
    drawArc(
        color = GoldAccent,
        startAngle = -90f,
        sweepAngle = 360f * fill.coerceIn(0f, 1f),
        useCenter = false,
        topLeft = androidx.compose.ui.geometry.Offset(ringC.x - ringR, ringC.y - ringR),
        size = androidx.compose.ui.geometry.Size(ringR * 2f, ringR * 2f),
        style = Stroke(width = unit * 0.020f, cap = StrokeCap.Round),
    )
    if (fill > 0.35f) {
        val a = ((fill - 0.35f) / 0.35f).coerceIn(0f, 1f)
        val check = Path().apply {
            moveTo(ringC.x - ringR * 0.46f, ringC.y + ringR * 0.02f)
            lineTo(ringC.x - ringR * 0.10f, ringC.y + ringR * 0.40f)
            lineTo(ringC.x + ringR * 0.50f, ringC.y - ringR * 0.36f)
        }
        drawPath(
            check,
            GoldAccent.copy(alpha = a),
            style = Stroke(width = unit * 0.022f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
