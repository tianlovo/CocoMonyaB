#!/bin/bash
prev_category=""
echo "# TDLib Update Types Classification"
echo ""
echo "This document lists all TdApi.Update subclasses extracted from TDLib Java documentation."
echo ""
while IFS='|' read -r category class desc; do
    if [ "$category" != "$prev_category" ]; then
        echo ""
        echo "## $category"
        echo ""
        prev_category="$category"
    fi
    echo "- **$class**: $desc"
done < categorized.txt
