# API reference

Generated from the live OpenAPI documents by `python tools/api_docs.py`. Interactive documentation is served by every service at `/swagger-ui.html`; the raw specification is at `/v3/api-docs`.

All responses use one envelope: `{"code": 200, "message": "...", "data": ...}`. The `code` mirrors the HTTP status (400 validation, 401 not signed in, 403 not allowed, 404 not found, 409 conflict with current state, 429 too many attempts). Lists are paged with `pageNum` (from 1) and `pageSize`.

## Back office (choco-admin, port 8080)

Authentication: Staff JWT from `POST /admin/login`; every path needs a role that grants its resource pattern.

### Catalog references

| Method | Path | Description |
|---|---|---|
| POST | `/allergen/create` |  |
| POST | `/allergen/delete/{id}` |  |
| GET | `/allergen/list` |  |
| POST | `/allergen/update/{id}` |  |
| POST | `/brand/create` |  |
| POST | `/brand/delete/{id}` |  |
| GET | `/brand/list` | Search the global brand directory by name, region, stocking stage and visibility |
| GET | `/brand/regions` |  |
| GET | `/brand/stocking/summary` | Brands per stocking stage and the guide's recommended starting range |
| POST | `/brand/update/showStatus/{id}` |  |
| POST | `/brand/update/stocking/{id}` | Move a brand through sampling, authorization, stocking and rotation |
| POST | `/brand/update/{id}` |  |
| GET | `/brand/{id}` |  |
| POST | `/chocolateType/create` |  |
| POST | `/chocolateType/delete/{id}` |  |
| GET | `/chocolateType/list` |  |
| POST | `/chocolateType/update/{id}` |  |
| POST | `/flavor/create` |  |
| POST | `/flavor/delete/{id}` |  |
| GET | `/flavor/list` |  |
| POST | `/flavor/update/{id}` |  |
| POST | `/productCategory/create` |  |
| POST | `/productCategory/delete/{id}` |  |
| GET | `/productCategory/list/withChildren` |  |
| GET | `/productCategory/list/{parentId}` |  |
| POST | `/productCategory/update/flags/{id}` |  |
| POST | `/productCategory/update/{id}` |  |
| GET | `/productCategory/{id}` |  |

### Members, marketing and content

| Method | Path | Description |
|---|---|---|
| POST | `/coupon/create` |  |
| POST | `/coupon/delete/{id}` |  |
| POST | `/coupon/grant` |  |
| GET | `/coupon/list` |  |
| POST | `/coupon/update/{id}` |  |
| GET | `/coupon/{id}` |  |
| GET | `/couponHistory/list` |  |
| POST | `/help/article/create` |  |
| POST | `/help/article/delete/{id}` |  |
| GET | `/help/article/list` |  |
| POST | `/help/article/update/{id}` |  |
| POST | `/help/category/create` |  |
| POST | `/help/category/delete/{id}` |  |
| GET | `/help/category/list` |  |
| POST | `/help/category/update/{id}` |  |
| POST | `/home/slot/create` |  |
| POST | `/home/slot/delete` |  |
| GET | `/home/slot/list` |  |
| POST | `/home/slot/status/{id}` |  |
| POST | `/home/slot/update/{id}` |  |
| GET | `/member/list` |  |
| POST | `/member/updateStatus/{id}` |  |
| GET | `/member/{id}` |  |
| POST | `/member/{id}/points` |  |
| GET | `/member/{id}/points/history` |  |
| POST | `/memberLevel/create` |  |
| POST | `/memberLevel/delete/{id}` |  |
| GET | `/memberLevel/list` |  |
| POST | `/memberLevel/update/{id}` |  |
| POST | `/pairing/create` |  |
| POST | `/pairing/delete/{id}` |  |
| GET | `/pairing/groups` |  |
| GET | `/pairing/list` |  |
| POST | `/pairing/update/{id}` |  |
| POST | `/serving/create` |  |
| POST | `/serving/delete/{id}` |  |
| GET | `/serving/list` |  |
| POST | `/serving/update/{id}` |  |
| POST | `/subject/create` |  |
| POST | `/subject/delete` |  |
| GET | `/subject/list` |  |
| POST | `/subject/update/{id}` |  |
| GET | `/subject/{id}` |  |

### Operations

| Method | Path | Description |
|---|---|---|
| POST | `/bundle/create` |  |
| POST | `/bundle/delete/{id}` |  |
| GET | `/bundle/list` |  |
| POST | `/bundle/update/{id}` |  |
| POST | `/experience/session/cancel/{id}` | Cancel a session; every booking is cancelled and refunded |
| POST | `/experience/session/create` |  |
| GET | `/experience/session/list` |  |
| POST | `/experience/session/update/{id}` |  |
| GET | `/experience/session/{id}` |  |
| GET | `/experience/session/{id}/bookings` |  |
| GET | `/guide/launch-checklist` |  |
| GET | `/guide/list` | Guide items of one type (PRODUCTION, PRINTING, SALES_LAUNCH, SHOP_FORMAT, ROADMAP, SHOP_CONCEPT) |
| GET | `/guide/progress` |  |
| GET | `/guide/sources` |  |
| POST | `/guide/update/{id}` |  |
| GET | `/guide/{id}` |  |
| POST | `/preorderWindow/create` |  |
| POST | `/preorderWindow/delete/{id}` |  |
| GET | `/preorderWindow/list` |  |
| POST | `/preorderWindow/status/{id}` |  |
| POST | `/preorderWindow/update/{id}` |  |
| GET | `/preorderWindow/{id}` |  |
| POST | `/production/create` |  |
| GET | `/production/list` |  |
| GET | `/production/report/waste` | Waste against released output, per product |
| GET | `/production/{id}` |  |
| POST | `/production/{id}/reject` |  |
| POST | `/production/{id}/release` | Release a batch to stock after the allergen check, temper test and best-before date |
| POST | `/production/{id}/start` |  |
| POST | `/stock/adjust` | Add or remove units by hand (waste, production or correction) |
| POST | `/stock/count` | Stock-take: set the counted quantity |
| GET | `/stock/ledger/{productId}` |  |
| GET | `/stock/list` |  |
| GET | `/stock/low` |  |

### Orders

| Method | Path | Description |
|---|---|---|
| POST | `/order/delete` |  |
| GET | `/order/list` | Search orders by number, customer, status, fulfilment type, preorder flag and date range |
| POST | `/order/update/close` | Cancel orders; paid orders are refunded and restocked |
| POST | `/order/update/complete` |  |
| POST | `/order/update/confirmPayment` | Confirm cash at the counter or a bank transfer |
| POST | `/order/update/delivery` | Delivery orders: hand over to a courier with a tracking number |
| POST | `/order/update/invalid` |  |
| POST | `/order/update/note/{id}` |  |
| POST | `/order/update/ready` | Pickup orders: mark as ready on the counter |
| POST | `/order/update/receiverInfo/{id}` |  |
| GET | `/order/{id}` |  |
| GET | `/orderSetting` |  |
| POST | `/orderSetting/update` |  |
| GET | `/payment/list` |  |
| GET | `/personalization/list` |  |
| GET | `/personalization/{id}` |  |
| POST | `/personalization/{id}/cancel` |  |
| POST | `/personalization/{id}/complete` |  |
| POST | `/personalization/{id}/proof` | Send the next artwork proof (versions are kept) |
| POST | `/personalization/{id}/quote` | Quote the price and the tested lead time |
| POST | `/personalization/{id}/ready` |  |
| POST | `/personalization/{id}/reject` |  |
| POST | `/personalization/{id}/start` |  |
| GET | `/returnApply/list` |  |
| POST | `/returnApply/update/status/{id}` | Accept, reject or complete a return (completing writes the refund and can restock) |
| GET | `/returnApply/{id}` |  |
| POST | `/returnReason/create` |  |
| POST | `/returnReason/delete` |  |
| GET | `/returnReason/list` |  |
| POST | `/returnReason/update/{id}` |  |
| POST | `/shopLocation/create` |  |
| POST | `/shopLocation/delete/{id}` |  |
| GET | `/shopLocation/list` |  |
| POST | `/shopLocation/update/{id}` |  |

### Permissions

| Method | Path | Description |
|---|---|---|
| POST | `/menu/create` |  |
| POST | `/menu/delete/{id}` |  |
| GET | `/menu/list/{parentId}` |  |
| GET | `/menu/treeList` |  |
| POST | `/menu/update/{id}` |  |
| GET | `/menu/{id}` |  |
| POST | `/resource/create` |  |
| POST | `/resource/delete/{id}` |  |
| GET | `/resource/list` |  |
| GET | `/resource/listAll` |  |
| POST | `/resource/update/{id}` |  |
| GET | `/resource/{id}` |  |
| POST | `/resourceCategory/create` |  |
| POST | `/resourceCategory/delete/{id}` |  |
| GET | `/resourceCategory/listAll` |  |
| POST | `/resourceCategory/update/{id}` |  |
| POST | `/role/allocMenu` |  |
| POST | `/role/allocResource` |  |
| POST | `/role/create` |  |
| POST | `/role/delete/{id}` |  |
| GET | `/role/list` |  |
| GET | `/role/listAll` |  |
| GET | `/role/listMenu/{roleId}` |  |
| GET | `/role/listResource/{roleId}` |  |
| POST | `/role/update/{id}` |  |
| POST | `/role/updateStatus/{id}` |  |

### Products

| Method | Path | Description |
|---|---|---|
| POST | `/product/claim/delete/{claimId}` |  |
| POST | `/product/claim/verify/{claimId}` | Verify a claim once supplier or process evidence is on file |
| POST | `/product/create` |  |
| GET | `/product/list` | Search products by keyword, category, brand, kind, launch tier and flags |
| POST | `/product/update/deleteStatus` | Soft-delete products (blocked while they are part of an open order) |
| POST | `/product/update/newStatus` |  |
| POST | `/product/update/publishStatus` | Publish or unpublish. Publishing requires reviewed allergens and an active SKU. |
| POST | `/product/update/recommendStatus` |  |
| POST | `/product/update/{id}` |  |
| GET | `/product/{id}` | Full product detail with SKUs, flavors, allergens and claims |
| POST | `/product/{id}/claim` | Record a dietary or sourcing claim (not shown to shoppers until verified) |
| GET | `/product/{id}/contribution` | Price minus ingredient, packaging, labour and selling fees, per unit |

### Reports and files

| Method | Path | Description |
|---|---|---|
| POST | `/file/upload` | Upload an image (PNG, JPEG, WebP or GIF, up to 5 MB) and receive its URL |
| GET | `/report/contribution` | Per-product contribution after ingredient, packaging, labour and selling fees |
| GET | `/report/dashboard` | Today's revenue and orders, the order queue and items that need attention |
| GET | `/report/products/top` |  |
| GET | `/report/sales/daily` |  |

### Staff accounts

| Method | Path | Description |
|---|---|---|
| POST | `/admin/delete/{id}` |  |
| GET | `/admin/info` | Profile, roles and menu tree of the signed-in staff member |
| GET | `/admin/list` |  |
| POST | `/admin/login` | Sign in and receive a JWT |
| POST | `/admin/logout` | Sign out (tokens are stateless; the client discards its token) |
| POST | `/admin/password` | Change a password (own account needs the current password; super admins may reset others) |
| POST | `/admin/register` | Create a staff account |
| POST | `/admin/role/update` | Replace the roles of a staff account |
| GET | `/admin/role/{adminId}` |  |
| POST | `/admin/update/{id}` |  |
| POST | `/admin/updateStatus/{id}` |  |
| GET | `/admin/{id}` |  |

## Storefront (choco-portal, port 8085)

Authentication: Customer JWT from `POST /sso/login`; browsing endpoints are public.

### Account

| Method | Path | Description |
|---|---|---|
| GET | `/sso/getAuthCode` | Send a verification code to a phone number (one per minute). In development the code is returned. |
| GET | `/sso/info` |  |
| POST | `/sso/login` | Sign in and receive a JWT |
| POST | `/sso/profile` |  |
| POST | `/sso/register` | Create an account and sign in |
| POST | `/sso/resetPassword` | Reset a forgotten password with a phone verification code |

### Basket and orders

| Method | Path | Description |
|---|---|---|
| POST | `/cart/add` | Add to the basket. A build-a-box line needs `box`: the pieces that fill the chosen box size. |
| POST | `/cart/clear` |  |
| POST | `/cart/delete` |  |
| GET | `/cart/list` |  |
| POST | `/cart/update/quantity` |  |
| POST | `/order/cancel/{id}` |  |
| POST | `/order/confirm` | Price the basket without ordering: bundle, coupon, points, delivery and gift fees, notices |
| POST | `/order/confirmReceipt/{id}` | Confirm that you collected or received the order |
| POST | `/order/delete/{id}` |  |
| GET | `/order/detail/{id}` |  |
| POST | `/order/generate` | Place the order. Stock is reserved and the order must be paid within the payment window. |
| GET | `/order/list` |  |
| POST | `/payment/pay` | Pay for an order. Card payments are charged immediately; use cardToken tok_decline to test a failure. |
| POST | `/returnApply/create` |  |
| GET | `/returnApply/list` |  |
| GET | `/returnApply/reasons` |  |

### Browse

| Method | Path | Description |
|---|---|---|
| GET | `/brand/detail/{id}` |  |
| GET | `/brand/list` |  |
| GET | `/bundle/list` |  |
| GET | `/experience/list` | Upcoming tastings and workshops with the seats left |
| GET | `/guide/pairingGroups` |  |
| GET | `/guide/pairings` |  |
| GET | `/guide/serving` |  |
| GET | `/help/article/{id}` |  |
| GET | `/help/centre` |  |
| GET | `/home/content` | Everything the home page needs: banners, brands, new and recommended items, subjects, categories |
| GET | `/home/hotProductList` |  |
| GET | `/home/newProductList` |  |
| GET | `/home/recommendProductList` |  |
| GET | `/home/subject/{id}` |  |
| GET | `/home/subjectList` |  |
| GET | `/preorder/windows` | Open preorder windows with the capacity left |
| GET | `/product/categoryTreeList` |  |
| GET | `/product/detail/{id}` | Product page: SKUs, flavors, allergens, verified claims, bundles, preorder windows or sessions |
| GET | `/product/related/{id}` |  |
| GET | `/product/search` | Search published products. Filters: keyword, category, brand, kind, flavor, chocolate type, allergens to avoid, verified claim, price range |
| GET | `/product/{id}/reviews` |  |
| GET | `/shop/info` | Opening hours, delivery fees, pickup lead time, loyalty and return rules |

### Member centre

| Method | Path | Description |
|---|---|---|
| POST | `/member/address/add` |  |
| POST | `/member/address/delete/{id}` |  |
| GET | `/member/address/list` |  |
| POST | `/member/address/update/{id}` |  |
| POST | `/member/attention/add/{brandId}` |  |
| POST | `/member/attention/delete/{brandId}` |  |
| GET | `/member/attention/list` |  |
| POST | `/member/coupon/claim/{couponId}` |  |
| GET | `/member/coupon/claimable` | Coupons this member can still claim |
| GET | `/member/coupon/list` |  |
| GET | `/member/forYou` |  |
| GET | `/member/points` |  |
| GET | `/member/preference` |  |
| POST | `/member/preference` | Save favourite flavors for recommendations (requires consent: true) |
| POST | `/member/preference/delete` | Delete saved flavor preferences |
| POST | `/member/productCollection/add/{productId}` |  |
| POST | `/member/productCollection/clear` |  |
| POST | `/member/productCollection/delete/{productId}` |  |
| GET | `/member/productCollection/list` |  |
| POST | `/member/readHistory/clear` |  |
| GET | `/member/readHistory/list` |  |
| POST | `/member/review/create` |  |

### Personalisation and experiences

| Method | Path | Description |
|---|---|---|
| POST | `/experience/book` | Reserve seats; pay within the payment window to confirm |
| POST | `/experience/cancel/{bookingId}` |  |
| GET | `/experience/my` |  |
| POST | `/experience/pay/{bookingId}` |  |
| GET | `/personalization/list` |  |
| POST | `/personalization/submit` | Request a personalised piece (name plaque, monogram, message strip...). Staff reply with a quote and lead time. |
| GET | `/personalization/{id}` | A request with its artwork proofs and the date it can be ready |
| POST | `/personalization/{id}/approve` | Approve the artwork proof you were shown (version must match the latest proof) |
| POST | `/personalization/{id}/cancel` |  |
| POST | `/personalization/{id}/changes` |  |
| POST | `/personalization/{id}/payDeposit` | Pay the deposit after approving the proof |

## Search (choco-search, port 8081)

Authentication: Public reads; `/search/index/**` needs the `X-Search-Admin-Key` header.

### Search

| Method | Path | Description |
|---|---|---|
| GET | `/search` | Search with filters: category, brand, kind, flavor, chocolate type, allergens to avoid, verified claim, price range, sort |
| GET | `/search/engine` |  |
| POST | `/search/index/delete/{productId}` |  |
| POST | `/search/index/importAll` | Rebuild the whole index from the product catalog |
| POST | `/search/index/{productId}` |  |
| GET | `/search/recommend/{productId}` | Products similar to the given one |
| GET | `/search/related` | Related search: how the results spread over categories, brands, flavors and kinds |
| GET | `/search/simple` | Keyword search |

