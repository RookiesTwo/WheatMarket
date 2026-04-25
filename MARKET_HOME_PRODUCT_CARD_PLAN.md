# Market Home Product Card Temporary Plan

## Goal

Implement the market home listing area as a paginated board of pinned paper product cards. Each market listing should
look like an individual paper sheet nailed onto the wooden market board instead of a generic data card.

## Component Boundary

- Keep `WheatMarketHomeUI` responsible for page state, filters, sorting, network requests, and pagination.
- Add a reusable single-listing component that returns a `UIElement`.
- Do not make a large list-level widget yet. The list container remains part of `market_home`.

## Visual Rules

- Each listing uses one of `paper_with_wrinkles_0.png` through `paper_with_wrinkles_9.png` as its background.
- Select the paper variant deterministically from the listing's internal `marketItemID`.
- Render `nail.png` near the top of each paper card.
- Card content follows the intended structure:
    - sell/buy label at the top
    - item icon in the middle
    - price at the bottom
- Preserve strong readability before adding decorative random offsets or rotations.

## Pagination Rules

- Use pagination, not scrolling, for browsing many listings.
- The current backend page size is 10, so the first implementation should target 10 cards per page.
- If the final visual density is too crowded, change the backend page size and front-end layout together.

## Execution Steps

1. Add texture helpers for deterministic wrinkled paper selection and nail rendering.
2. Add a single-listing card component for pinned paper visuals.
3. Replace the current product card construction in `WheatMarketHomeUI` with the new component.
4. Rename the product area from scroller semantics to board semantics.
5. Verify Java inspections/build after the first implementation.
