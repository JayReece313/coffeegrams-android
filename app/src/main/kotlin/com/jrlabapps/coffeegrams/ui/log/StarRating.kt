package com.jrlabapps.coffeegrams.ui.log

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jrlabapps.coffeegrams.R

/**
 * A small 1-5 star control, ported from the iOS app's `StarRating.swift`. A
 * [rating] of 0 means "unrated". Read-only when [onRatingChange] is `null`
 * (the log list rows); editable when it's supplied (the log detail screen).
 *
 * Tapping the currently-set star clears the rating back to unrated, matching
 * the iOS behavior exactly rather than only ever increasing it.
 */
@Composable
fun StarRating(rating: Int, onRatingChange: ((Int) -> Unit)? = null, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (value in 1..5) {
            val filled = value <= rating
            val description = pluralStringResource(R.plurals.star_rating_star_count, value, value)
            Icon(
                imageVector = if (filled) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = null,
                tint = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(size)
                    .then(
                        if (onRatingChange != null) {
                            Modifier.clickable(onClickLabel = description) {
                                onRatingChange(if (rating == value) 0 else value)
                            }
                        } else {
                            Modifier
                        },
                    )
                    .semantics { contentDescription = description },
            )
        }
    }
}
