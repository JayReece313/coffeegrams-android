package com.jrlabapps.coffeegrams.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// The iOS app pairs the system font (SF Pro) with Dynamic Type; a dedicated
// rounded/branded display face is explicitly TBD there too (DESIGN.md, M11),
// so the system default sans-serif is used here rather than adding a font
// dependency this milestone doesn't call for.
//
// Only displayLarge is overridden — it carries the dose/ratio numerals, "the
// reason the screen exists" per DESIGN.md, sized to iOS's 48pt hero readout.
// Every other role is Material3's own default type scale.
val CoffeeGramsTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
    ),
)
