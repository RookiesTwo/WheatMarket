# Market Home Product Card Status

## Current Implementation

- `WheatMarketHomeUI` owns page state, filters, sorting, localized search, balance display, network requests, and
  pagination.
- `MarketListingCard.create(...)` returns a reusable `UIElement` for one market listing.
- The product board is a normal LDLib2 container in `market_home.xml`; pagination is used instead of scrolling.
- Page size is calculated from the visible board area using `64x64` cards plus gaps and is capped at `64`, not fixed at
  `10`.
- Each card selects `paper_with_wrinkles_0.png` through `paper_with_wrinkles_9.png` deterministically from
  `marketItemID`.
- Current card content is intentionally minimal: sell/buy label, item icon, and price.
- Card click is wired through `WheatMarketHomeUI` into the current order confirmation flow.

## Differences From The Original Temporary Plan

- `nail.png` exists as an asset, but the current `MarketListingCard` does not render a nail element.
- The backend page size is no longer a hardcoded `10`; the client requests a calculated page size based on the board
  layout.
- The product area has already been renamed away from scroller semantics in code (`productBoard.clearAllChildren()` /
  `addChild(...)`).

## Remaining Work

- Add stock, seller/system-shop marker, listing time, last trade time, and cooldown/limit state to the card or tooltip.
- Decide whether to actually render `nail.png` on cards or remove it from the visual plan.
- Improve buy-order behavior: current click path reaches the confirmation UI, where buy orders are rejected as
  unsupported.
- Keep pagination and card layout changes coordinated with `WheatMarketHomeUI.calculateProductsPerPage()` and
  `MarketService.MAX_ITEMS_PER_PAGE`.
