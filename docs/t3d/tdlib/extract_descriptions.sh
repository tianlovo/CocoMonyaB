#!/bin/bash
while IFS= read -r class; do
    file="org/drinkless/tdlib/TdApi.$class.html"
    if [ -f "$file" ]; then
        # Extract description from block div
        desc=$(grep -A1 'class="block"' "$file" | sed -n 's/.*<div class="block">\(.*\)<\/div>.*/\1/p' | head -1)
        if [ -z "$desc" ]; then
            desc="No description available"
        fi
        echo "$class|$desc"
    else
        echo "$file not found"
    fi
done < updates.txt
