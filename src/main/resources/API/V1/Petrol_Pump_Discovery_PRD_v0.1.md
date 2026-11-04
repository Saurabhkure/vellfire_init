# Petrol Pump Discovery Application

## Business Requirements Document (BRD) and Development Requirements

**Document version:** 0.2\
**Status:** Updated draft --- open decisions require product-owner
confirmation\
**Last updated:** 3 November 2026\
**Primary audience:** Product owner, UI/UX designer, backend developer,
frontend developer, QA and deployment team\
**Existing technology direction:** Java 21, Spring Boot and an
integrated web frontend

------------------------------------------------------------------------

## 1. Purpose of this document

This document updates the existing Petrol Pump Discovery Application BRD
using the latest product direction.

The first release is intended to help a user discover nearby petrol
pumps, compare them using **driving distance and estimated driving
time**, and review available station information before choosing a
station.

The agreed provider responsibilities for the current scope are:

-   **Google Places API:** Discover nearby petrol pumps and return
    available place details, including coordinates, navigation points
    and Google rating where returned.
-   **Ola Maps Distance Matrix Basic API:** Calculate estimated driving
    distance and duration from the user's location to each selected
    petrol pump.
-   **Application logic:** Normalize provider responses, calculate a
    tentative left/right side indication where sufficient route geometry
    is available, rank stations by driving distance, and return a
    consistent response to the frontend.

This document is intended to guide development. Items that have not been
confirmed are explicitly marked **TBD --- product-owner decision
required**. Developers must not silently choose values, infer missing
station information, or treat tentative classifications as verified
facts.

## 2. Product vision

Help a driver identify and compare nearby petrol pumps based on how far
and how long it is expected to take to reach them by road, while
presenting useful station details and a clearly qualified indication of
which side of the final approach the station appears to be on.

The product is not intended to be only a list sorted by straight-line
proximity. The primary distance used for ranking is the driving distance
returned by Ola Maps Distance Matrix.

## 3. Goals

1.  Allow a user to start a nearby petrol pump search from the web
    application.
2.  Obtain the user's location after the required permission or
    location-input flow is completed.
3.  Discover nearby petrol pumps through Google Places API.
4.  Obtain estimated driving distance and duration for candidate
    stations through Ola Maps Distance Matrix Basic.
5.  Rank stations in ascending order of returned driving distance.
6.  Display the following information when reliable source data is
    available:
    -   Petrol pump name.
    -   Driving distance.
    -   Estimated driving time.
    -   Tentative side of the final approach: left, right or unknown.
    -   Washroom availability: available, unavailable or unknown.
    -   Fuel types available.
    -   Google rating.
7.  Make missing or uncertain information visible as unavailable/unknown
    rather than guessing.
8.  Provide a Google Maps hand-off for a selected station, subject to
    the applicable Google Maps Platform requirements.

## 4. Non-goals for this release

The following are not part of the current first-release scope unless
separately approved:

-   Nationwide owned fuel-station database and bulk third-party dataset
    ingestion.
-   PostGIS-based nationwide search infrastructure.
-   Own map-tile hosting or a custom road-map rendering platform.
-   Fleet management, B2B dashboards or commercial station-owner
    subscriptions.
-   Fuel-price comparison or live fuel-price feeds.
-   Fuel purchase, payments or booking.
-   User accounts, favourites, reviews or user-submitted amenity
    verification.
-   Guaranteed verification that a station is open, dispensing fuel or
    physically accessible.
-   Guaranteed identification of a physical driveway, divider opening,
    U-turn or service-road connection from the available API data alone.

## 5. Users and primary use case

### 5.1 Target user

A driver or passenger who wants to find a nearby petrol pump and compare
the practical effort required to reach the available options.

### 5.2 Primary use case

1.  User opens the web application.
2.  Application explains why location is needed and obtains the user's
    location, or follows the separately approved location-entry flow.
3.  Frontend sends the origin coordinates to the application backend.
4.  Backend searches Google Places for nearby petrol pumps.
5.  Backend normalizes the returned place records and selects a routing
    destination for each station.
6.  Backend calls Ola Maps Distance Matrix Basic for the
    origin-to-station pairs.
7.  Backend calculates a tentative side-of-final-approach indication
    when the required navigation point and route polyline data are
    available.
8.  Backend ranks stations by driving distance in ascending order.
9.  Frontend displays the station cards and available details.
10. User selects a station and opens its Google Maps listing or
    directions action, according to the confirmed navigation behaviour.

**Open wording to confirm:** The latest user flow says "Ask for the
directions." It is not yet clear whether this means (a) request browser
location permission, (b) ask the user to enter a starting location, or
(c) ask the user to enter both an origin and destination. See Open
Question Q-01.

## 6. Agreed provider responsibilities

  --------------------------------------------------------------------------------
  Provider / component    Responsibility in this  Must not be used to claim
                          release                 
  ----------------------- ----------------------- --------------------------------
  Browser Geolocation or  Obtain the user's       A location if permission/input
  approved location input origin coordinates      failed

  Google Places API       Discover petrol pumps   That every returned station is
                          and return place        operational or every amenity is
                          details available under known
                          the selected API fields 

  Ola Maps Distance       Return estimated        That the matrix result verifies
  Matrix Basic            driving distance and    the station entrance or
                          duration for            guarantees traffic conditions
                          origin-destination      
                          pairs; return route     
                          polyline where          
                          supported and present   
                          in the response         

  Application             Estimate left/right     Confirmed
  side-analysis logic     relative to the final   same-side/opposite-carriageway
                          route approach when its access or verified physical
                          inputs are adequate     entrance

  Application ranking     Sort routable stations  A recommendation based on
  logic                   by driving distance     unapproved weighting factors
                          ascending               
  --------------------------------------------------------------------------------

### 6.1 Terminology

-   **Origin:** The user's current or explicitly entered starting
    coordinate.
-   **Place location:** The main coordinate returned for the petrol pump
    by Google Places.
-   **Navigation point:** A coordinate in `places[].navigationPoints[]`,
    including its `travelModes` and optional `usages`.
-   **Routing destination:** The coordinate selected by the backend for
    the Ola Matrix request.
-   **Driving distance:** The distance returned by Ola Maps for the road
    route, in metres.
-   **Estimated duration:** The duration returned by Ola Maps, in
    seconds.
-   **Final approach:** The direction of travel along the last usable
    segment of the returned route polyline.
-   **Tentative side:** The estimated position of the petrol pump's main
    place coordinate relative to the final approach direction. It is not
    a verified entrance-side classification.

## 7. End-to-end functional flow

### Step 1 --- User opens the website

The application displays the petrol pump discovery landing page and
explains the purpose of location access.

### Step 2 --- Obtain origin location

The application follows the approved origin-capture behaviour.

-   If browser geolocation is approved, obtain latitude and longitude.
-   If the user denies permission, show a clear explanation and the
    approved retry or alternative-location behaviour.
-   If location cannot be determined, show an error and do not send
    invalid coordinates to the backend.

**TBD:** Whether manual origin entry is required in the first release.

### Step 3 --- Frontend calls application backend

The frontend sends the origin coordinates and only those search
parameters that have been approved.

The browser must call the application's backend, not expose a
server-side provider API key in frontend code.

### Step 4 --- Google Places nearby search

The backend calls Google Places API to discover petrol pumps around the
origin.

The backend must: - Use the agreed Google Places API version and search
method. - Request only the required fields through the field mask. -
Handle pagination if the selected endpoint and result set require it. -
Preserve the Google place identifier for station identity and Maps
hand-off where permitted. - Normalize the provider response into the
application's internal station model. - Handle missing coordinates,
missing navigation points and missing optional fields. - Respect
Google's current attribution, storage, caching and display terms.

**TBD:** Search radius, result limit, pagination policy, exact Places
endpoint, exact field mask and whether any additional search filters are
required.

### Step 5 --- Select a routing destination for each petrol pump

For each normalized station:

1.  Inspect `navigationPoints`, if present.
2.  Prefer a navigation point whose `travelModes` contains `DRIVE` and
    whose `usages` do not identify it as `PARKING`.
3.  If the only available DRIVE navigation point is marked `PARKING`, it
    may be used only if the product owner approves that fallback; record
    that the destination is a parking point and lower the side-analysis
    confidence.
4.  If no usable DRIVE navigation point is available, use the main
    `places[].location` coordinate only if the product owner approves
    this fallback; mark the destination source as `PLACE_LOCATION`.
5.  If neither coordinate is available or valid, do not send that
    station to Ola Matrix.

The Google `navigationPointToken` is retained as provider metadata if
needed, but the Ola Matrix request must use the destination format
supported by Ola. Do not assume Ola accepts Google's navigation-point
token.

**TBD:** Approval of the fallback rules in items 3 and 4.

### Step 6 --- Ola Maps Distance Matrix Basic

The backend sends the user's origin and the selected petrol pump routing
destinations to Ola Maps Distance Matrix Basic.

For every origin-destination result, normalize: - Element status. -
Driving distance in metres. - Estimated duration in seconds. - Polyline,
if returned. - Provider error information, if present.

The sample Ola Matrix response previously supplied for this project
contains `distance`, `duration`, `polyline` and `status` inside each
`rows[].elements[]` item. The implementation must validate the actual
response and must not assume every element is successful or contains
every optional field.

The application must: - Match each matrix element to the correct petrol
pump. - Check the element status before using distance, duration or
polyline. - Handle partial failures where some stations succeed and
others fail. - Respect Ola's current request limits, authentication
rules and usage/pricing terms. - Keep the Ola API key on the backend.

**TBD:** Maximum station count per matrix request, batching policy,
timeout, retry policy and behaviour for a station whose matrix element
is not successful.

### Step 7 --- Tentative side analysis

The application calculates a tentative left/right indication using the
final approach segment from the Ola route polyline and the offset from
that route endpoint to the petrol pump's main place coordinate.

The algorithm is specified in Section 8.

If any required input is missing, invalid or too imprecise, the result
must be `UNKNOWN`. The application must not invent a side.

### Step 8 --- Ranking

For stations with successful Ola Matrix results:

1.  Sort by `distanceMeters` ascending.
2.  Display the estimated duration as supporting information.
3.  Do not silently prioritize Google rating, washroom, fuel type, open
    status or side classification over driving distance.
4.  Use a stable tie-breaker only after the product owner approves it.

Stations with unsuccessful or unavailable routing results must be
handled according to the decision required in Q-08. They must not be
assigned a fabricated distance.

### Step 9 --- Results display

Each station card should display the fields approved in Section 9.
Missing values must be shown as `Unknown`, `Not available` or omitted
according to the approved UI rule. A missing value must never be
presented as a negative fact.

### Step 10 --- Maps hand-off

The station card provides a Google Maps action. The exact action---open
the place listing or open directions from the user's origin---must be
confirmed.

## 8. Tentative left/right side-detection algorithm

### 8.1 Purpose

Provide a useful, explicitly tentative indication of whether the petrol
pump's main place coordinate is to the left or right of the vehicle's
final approach direction.

This is not a guarantee that the vehicle can enter the station from that
side. It does not, by itself, prove that a station is on the same or
opposite carriageway of a divided highway, nor does it verify a
service-road entrance.

### 8.2 Required inputs

For a station, the algorithm requires:

1.  User origin coordinate.
2.  Selected routing destination coordinate.
3.  Main Google Places petrol pump coordinate.
4.  Successful Ola Matrix element.
5.  A decodable route polyline containing at least two distinct route
    points.

The selected navigation point should be the routing destination where an
approved DRIVE navigation point exists. The main place coordinate is
retained separately because it is used as the point whose side is being
estimated.

### 8.3 Algorithm

**A. Decode the route polyline**

-   Decode the successful matrix element's polyline into an ordered
    sequence of latitude/longitude points.
-   Validate that the sequence contains at least two distinct points.
-   Remove consecutive duplicate points or zero-length segments for
    final-segment selection.
-   If the polyline is absent, invalid or contains no usable segment,
    return `UNKNOWN`.

**B. Determine the final approach vector**

-   Let `R1` be the penultimate usable route point.
-   Let `R2` be the final usable route point.
-   The directed final-approach vector is from `R1` to `R2`.
-   The vector must follow the route's travel order, not an arbitrary
    coordinate order.

**C. Determine the station offset vector**

-   Let `P` be the main Google Places coordinate for the petrol pump.
-   Let `R2` be the route endpoint.
-   Calculate the offset vector from `R2` to `P`.

**D. Convert coordinates to a local planar coordinate system**

For a small local area, convert longitude/latitude differences into a
local east/north coordinate system around the route endpoint. Longitude
must be adjusted for latitude (for example, using the cosine of the
reference latitude) so that the cross-product calculation is not
performed directly on unscaled degree values.

Use: - `x` as local east-west distance. - `y` as local north-south
distance.

Let: - `D = (dx, dy)` be the final-approach vector from `R1` to `R2`. -
`S = (sx, sy)` be the offset vector from `R2` to the station coordinate
`P`.

**E. Calculate the 2D cross product**

`cross = (dx * sy) - (dy * sx)`

With the coordinate convention above and the route vector in direction
of travel: - `cross > 0`: the station point is on the left of the final
approach vector. - `cross < 0`: the station point is on the right of the
final approach vector. - `cross` near zero: the point is approximately
on the route line; classification is ambiguous.

The implementation must validate the sign convention with known
northbound, southbound, eastbound and westbound test cases before
release.

**F. Apply a tolerance and confidence rules**

Do not classify based on the sign alone. GPS and provider coordinates
can be imprecise, and a very small offset may be meaningless.

The implementation must use a configurable tolerance / minimum
lateral-offset threshold. The threshold value is **TBD and must be
calibrated using test locations**; it must not be invented during
implementation.

Return `UNKNOWN` when: - The route polyline is missing or cannot be
decoded. - Fewer than two distinct route points are available. - The
main place coordinate is missing or invalid. - The final approach vector
is too short or unstable. - The lateral offset is below the approved
threshold. - The station coordinate and route endpoint are too close to
distinguish a side reliably. - The selected routing destination is a
low-confidence fallback and the product rules say not to classify it. -
The geometry produces an ambiguous result.

### 8.4 Output values

The initial side-analysis output should be one of:

-   `LEFT_OF_FINAL_APPROACH`
-   `RIGHT_OF_FINAL_APPROACH`
-   `UNKNOWN`

The UI label should be understandable to the user, for example: -
`Likely on your left` - `Likely on your right` - `Side not available`

The UI must include wording or visual treatment that makes the result
tentative, not verified.

### 8.5 Confidence and provenance

Store enough metadata to explain how the result was obtained:

-   `sideEstimate`
-   `sideConfidence` (the confidence scale and thresholds are TBD)
-   `sideMethod`, for example `FINAL_APPROACH_POLYLINE_CROSS_PRODUCT`
-   `routingDestinationSource` (`DRIVE_NAVIGATION_POINT`, approved
    parking-point fallback, or approved place-location fallback)
-   `sideReason` for `UNKNOWN` or reduced confidence.

Do not invent a numerical confidence score until the product owner
approves how it is calculated and validated.

### 8.6 Important limitations

This algorithm estimates the side of the main place coordinate relative
to the last route segment. It does **not** independently establish: -
Whether a divider separates the petrol pump from the user's
carriageway. - Whether a legal U-turn is available or required. -
Whether a service road is the actual access road. - Whether the mapped
navigation point is the physical driveway. - Whether a vehicle can enter
the station from the side indicated.

If the product later needs confirmed
same-side/opposite-side/service-road classification, a separately
approved road-network data source and algorithm will be required. That
work is not silently included in this first-release side estimate.

## 9. Station card data requirements

  ----------------------------------------------------------------------------
  Field             Required for      Source / rule     Missing-data behaviour
                    current UI                          
  ----------------- ----------------- ----------------- ----------------------
  Station name      Yes               Google Places     Do not create a name
                                      result            

  Google place ID   Backend identity  Google Places     Handle according to
                    / Maps hand-off                     API response

  Address           TBD whether       Google Places, if Omit or show
                    visible           returned          unavailable

  Driving distance  Yes               Ola Matrix        Do not display a
                                      `distance`        fabricated value

  Estimated driving Yes, subject to   Ola Matrix        Show unavailable if
  time              confirmation      `duration`        element fails

  Tentative side    Yes, when         Section 8         `UNKNOWN` /
                    calculable        algorithm         `Side not available`

  Washroom          Requested field;  Must come from an `UNKNOWN`; absence is
  availability      source not        explicitly        not `NO`
                    confirmed         approved reliable 
                                      field/source      

  Fuel types        Requested field;  Use only          `UNKNOWN` / not
  available         response field    explicitly        available
                    mapping to be     returned          
                    confirmed         fuel-type data    
                                      from an approved  
                                      source            

  Google rating     Yes, when         Google Places     Not available if
                    returned          rating field      missing

  Google rating     Optional; confirm Google Places     Omit if missing
  count             whether to        user-rating       
                    display           count, if         
                                      returned and      
                                      approved          

  Open/closed       Carried over from Google Places     `UNKNOWN`, never infer
  status            previous BRD;     current opening   closed
                    retention not     status, if        
                    confirmed in      returned          
                    latest                              
                    requirements                        

  Google Maps       Existing          Google place      Show only if a valid
  action            requirement;      identifier or     destination can be
                    exact action to   approved Maps URL built
                    confirm                             
  ----------------------------------------------------------------------------

### 9.1 Washroom availability

The application must not infer washroom availability from: - The station
brand. - The station name. - A nearby business. - A missing field. - The
fact that the station is on a highway.

A value of `YES` or `NO` may only be returned when an approved source
explicitly supports that value. Otherwise return `UNKNOWN`.

**TBD:** The approved washroom data source, verification method,
freshness rule and whether washroom availability is a launch-blocking
field.

### 9.2 Fuel types

The application must show only fuel types explicitly returned by the
approved provider response or another approved data source. It must not
infer fuel types from a brand or station name.

**TBD:** Confirm the exact Google Places response field and field-mask
support for fuel types in the API version being used. If the current
response does not supply fuel types, an alternative source must be
approved before development can treat this as a populated field.

### 9.3 Google rating

Use the rating returned by Google Places, if requested and returned in
the selected field mask. Do not calculate or modify Google's rating.
Whether to display the review count is TBD.

All Google Places content must be displayed, attributed, stored or
cached only as permitted by the applicable Google Maps Platform terms.

## 10. Ranking rules

### 10.1 Primary ranking

The primary ranking rule is:

**Successful Ola Matrix results sorted by `distanceMeters` ascending.**

The displayed distance is road/driving distance from Ola Matrix, not
Haversine or straight-line distance.

### 10.2 Estimated time

Display the Ola Matrix duration in a readable format. Duration is
informational and is not a ranking factor in the current stated
requirement unless the product owner changes the rule.

### 10.3 Other station attributes

Google rating, washroom availability, fuel type, side estimate and
opening status are displayed as supporting information. They must not
change the ranking unless a future ranking rule is explicitly approved.

### 10.4 Ties and route failures

-   Tie-breaking rule for equal distances: **TBD**.
-   Placement of stations with failed/unavailable Matrix results:
    **TBD**.
-   Whether stations without a DRIVE navigation point can still be
    ranked using the main place location: **TBD**.

## 11. Functional requirements

  -----------------------------------------------------------------------
  ID                      Requirement             Expected behaviour /
                                                  acceptance point
  ----------------------- ----------------------- -----------------------
  FR-01                   Start search            User can start a petrol
                                                  pump search from the
                                                  web application.

  FR-02                   Obtain origin           Application obtains
                                                  valid user origin
                                                  coordinates using the
                                                  approved location flow.

  FR-03                   Location failure        Permission denial,
                                                  unavailable location
                                                  and timeout are handled
                                                  with clear UI feedback.

  FR-04                   Google Places search    Backend searches for
                                                  petrol pumps around the
                                                  origin using approved
                                                  search parameters.

  FR-05                   Place normalization     Backend maps provider
                                                  results into a stable
                                                  internal station model.

  FR-06                   Navigation-point        Backend applies the
                          selection               approved DRIVE
                                                  navigation-point
                                                  selection and fallback
                                                  rules.

  FR-07                   Ola Matrix request      Backend requests
                                                  distance and duration
                                                  for the
                                                  origin-to-station
                                                  pairs.

  FR-08                   Matrix result mapping   Each matrix element is
                                                  mapped to the correct
                                                  station and its status
                                                  is checked.

  FR-09                   Driving distance        Successful station
                                                  results contain Ola
                                                  driving distance in
                                                  metres.

  FR-10                   Estimated time          Successful station
                                                  results contain Ola
                                                  duration in seconds.

  FR-11                   Side estimate           Backend calculates
                                                  left/right/unknown
                                                  using Section 8 when
                                                  required inputs exist.

  FR-12                   Washroom                Backend returns only
                                                  source-supported
                                                  washroom values;
                                                  otherwise `UNKNOWN`.

  FR-13                   Fuel types              Backend returns only
                                                  source-supported fuel
                                                  types; otherwise
                                                  unavailable/unknown.

  FR-14                   Google rating           Backend returns the
                                                  Google rating when
                                                  present and permitted
                                                  for use.

  FR-15                   Ranking                 Routable stations are
                                                  sorted by driving
                                                  distance ascending.

  FR-16                   Station cards           Frontend displays the
                                                  approved fields and
                                                  handles unavailable
                                                  values clearly.

  FR-17                   Maps hand-off           User can open the
                                                  selected station in the
                                                  approved Google Maps
                                                  destination/action.

  FR-18                   Partial provider        Successful station
                          failure                 results can be
                                                  distinguished from
                                                  failed station results;
                                                  no distance is
                                                  fabricated.

  FR-19                   Responsive UI           Search and station-card
                                                  functionality work on
                                                  agreed mobile and
                                                  desktop browsers.

  FR-20                   Provider credentials    Google and Ola
                                                  server-side API keys
                                                  are not exposed in
                                                  browser code or
                                                  committed to source
                                                  control.
  -----------------------------------------------------------------------

## 12. Conceptual data model

The following is a logical model, not a final database schema. Whether
persistence is required for the first release is still to be confirmed.

### 12.1 Normalized station result

  -----------------------------------------------------------------------
  Field                               Type / meaning
  ----------------------------------- -----------------------------------
  `placeId`                           Google place identifier

  `name`                              Station name

  `address`                           Optional formatted address

  `placeLatitude`                     Main place latitude

  `placeLongitude`                    Main place longitude

  `routingLatitude`                   Selected Ola destination latitude

  `routingLongitude`                  Selected Ola destination longitude

  `routingDestinationSource`          Navigation point or approved
                                      fallback source

  `navigationTravelModes`             Provider travel modes, if returned

  `navigationUsages`                  Provider usages, if returned

  `drivingDistanceMeters`             Ola Matrix distance

  `estimatedDurationSeconds`          Ola Matrix duration

  `routePolyline`                     Ola Matrix polyline, if returned
                                      and retained for immediate analysis

  `sideEstimate`                      Left / right / unknown

  `sideConfidence`                    TBD confidence representation

  `sideMethod`                        Algorithm identifier

  `washroomAvailability`              Yes / no / unknown

  `fuelTypes`                         List of explicitly supported fuel
                                      types

  `googleRating`                      Google Places rating, if returned

  `googleRatingCount`                 Optional count, subject to approval

  `openStatus`                        Open / closed / unknown, if
                                      retained

  `matrixStatus`                      Provider element status

  `mapsDestination`                   Approved Maps hand-off target
  -----------------------------------------------------------------------

### 12.2 Persistence

The previous BRD proposed that a database might not be required for a
stateless live-search MVP. That remains a decision.

The implementation must not persist Google Places content unless the
applicable Google terms permit the specific storage. The team must
separately decide whether to store only permitted identifiers,
application-derived values, or other approved data.

## 13. Conceptual backend API

The final endpoint names, request/response schema and validation rules
must be agreed during technical design.

A logical backend operation is:

**Nearby petrol pump search** - Input: origin latitude and longitude,
plus only approved search parameters. - Processing: Google Places search
→ normalization → routing-destination selection → Ola Matrix → side
estimate → distance ranking. - Output: normalized station results,
search metadata and provider/partial-failure status.

The frontend must call the application's backend. Provider credentials
remain server-side.

## 14. Non-functional requirements

  -----------------------------------------------------------------------
  Area                                Requirement
  ----------------------------------- -----------------------------------
  Performance                         Search should return within a
                                      response-time target agreed by the
                                      product owner; show loading
                                      feedback while calls are in
                                      progress.

  Reliability                         Handle Google and Ola timeouts,
                                      quota errors, partial failures and
                                      invalid responses.

  Security                            Store API keys in
                                      environment-specific configuration
                                      or a secrets manager; use HTTPS in
                                      deployed environments.

  Privacy                             Request user location only for the
                                      stated search purpose. Define
                                      whether origin coordinates are
                                      logged or retained.

  Data quality                        Do not infer missing station
                                      amenities, fuel types, ratings or
                                      access-side classifications.

  Explainability                      Side estimates must be labelled
                                      tentative and expose `UNKNOWN` when
                                      evidence is insufficient.

  Maintainability                     Separate controller, provider
                                      clients, normalization, routing
                                      orchestration, side analysis and
                                      ranking responsibilities.

  Configuration                       Search radius, result limit, API
                                      timeouts, retry policy and
                                      side-analysis tolerance must be
                                      configurable once their values are
                                      approved.

  Cost control                        Request only necessary Google
                                      Places fields; batch Ola Matrix
                                      requests within provider limits;
                                      monitor provider usage and billing.

  Compatibility                       Support the agreed desktop and
                                      mobile browsers; exact browser
                                      support matrix is TBD.
  -----------------------------------------------------------------------

## 15. Error and edge-case handling

1.  **Location permission denied:** Explain the issue and offer only the
    approved retry/manual-location option.
2.  **Location unavailable or timeout:** Show a clear message; do not
    send invalid coordinates.
3.  **Invalid coordinates:** Reject latitude outside `[-90, 90]` or
    longitude outside `[-180, 180]`.
4.  **No Google Places results:** Show a no-stations-found message for
    the configured search area.
5.  **Google Places partial/missing fields:** Keep the station if
    required fields exist; represent optional fields as
    unknown/unavailable.
6.  **No navigation point:** Apply only the approved fallback rule.
7.  **Only parking navigation point:** Apply only the approved
    parking-point rule and lower confidence if permitted.
8.  **Ola Matrix element failure:** Do not display fabricated
    distance/time; apply the approved failed-route result policy.
9.  **Polyline absent or invalid:** Side estimate must be `UNKNOWN`;
    distance/time may still be displayed if valid.
10. **Ambiguous side calculation:** Return `UNKNOWN`.
11. **Washroom data absent:** Return `UNKNOWN`, not `NO`.
12. **Fuel type data absent:** Return unavailable/unknown; do not infer
    from brand.
13. **Google rating absent:** Omit or show unavailable; do not
    substitute another rating.
14. **Duplicate place results:** Use Google place identifiers for
    duplicate handling where permitted.
15. **Provider quota or outage:** Show a temporary service error and do
    not represent stale provider results as current results.

## 16. Security, privacy and provider compliance

-   Google Places and Ola API keys must be backend-only for
    server-to-server calls.
-   Restrict keys by API, environment and server/application as
    supported by each provider.
-   Configure quota and billing alerts.
-   Do not commit secrets to source control.
-   Use HTTPS in production.
-   Explain location use before or alongside the browser permission
    prompt.
-   Define location logging and retention before launch.
-   Follow current Google Places attribution, caching, storage and
    map-display requirements.
-   Follow current Ola Maps API usage, request-limit and pricing
    requirements.
-   Do not assume that Google Places content can be copied into an
    independent database or displayed on a non-Google map without
    checking the applicable terms.

## 17. Acceptance criteria

The first release is ready for product-owner acceptance when:

1.  A user can open the application and complete the approved
    origin-location flow.
2.  Valid origin coordinates reach the backend.
3.  Backend returns nearby petrol pump records from Google Places.
4.  Each station is normalized and associated with the correct place
    identifier.
5.  Routing destination selection follows the approved navigation-point
    rules.
6.  Ola Matrix returns successful distance/duration data for routable
    stations.
7.  The application maps each Matrix element to the correct station.
8.  Successful stations are sorted by driving distance ascending.
9.  Station cards show driving distance and estimated duration.
10. Side estimate is shown only when the approved algorithm has
    sufficient valid input; otherwise the UI shows unknown/unavailable.
11. Washroom and fuel types are shown only when supported by an approved
    source.
12. Google rating is shown only when returned and permitted.
13. Missing information is never presented as a negative fact.
14. Location errors, no results, provider errors and partial Matrix
    failures are handled.
15. API keys are not exposed in frontend code.
16. The station Maps action opens the approved destination.
17. Side-estimate behaviour has been validated against a labelled test
    set covering multiple travel directions and ambiguous/offset cases.

## 18. Delivery plan

  -----------------------------------------------------------------------
  Phase                   Work                    Exit criteria
  ----------------------- ----------------------- -----------------------
  1\. Requirement         Resolve open questions  Product owner approves
  decisions               in Section 19; approve  the requirements and
                          provider fields and     field mapping.
                          fallback rules.         

  2\. Google Places       Implement backend       Valid sample stations
  integration             Places search and       are returned with
                          normalized station      mapped fields.
                          model.                  

  3\. Ola Matrix          Select routing          Distance and duration
  integration             destinations and        are correctly mapped to
                          request matrix results. each station.

  4\. Side-analysis       Decode polyline,        Labelled test cases
  implementation          calculate final         pass; ambiguous cases
                          approach and apply      return unknown.
                          approved                
                          tolerance/confidence    
                          rules.                  

  5\. Ranking and         Sort by driving         Stable response
  response API            distance and return     contract and ordering
                          normalized data.        are verified.

  6\. Frontend            Location flow,          Responsive user flow
                          loading/error states,   works on agreed
                          station cards and Maps  devices.
                          action.                 

  7\. Validation and      Provider quotas,        Product owner accepts
  launch readiness        privacy, attribution,   the MVP.
                          edge cases and          
                          acceptance checks.      
  -----------------------------------------------------------------------

## 19. Open questions --- product-owner decisions required

The following decisions are not specified sufficiently for a development
team to finalize implementation. Do not silently assume answers.

  -------------------------------------------------------------------------
  ID                      Question                  Why it is required
  ----------------------- ------------------------- -----------------------
  Q-01                    In step 2, do you mean    Defines the first user
                          request browser location  interaction and
                          permission, let the user  frontend flow.
                          enter a starting          
                          location, or ask for both 
                          origin and destination?   

  Q-02                    What is the default       Defines the candidate
                          Google Places search      station search area.
                          radius, and should the    
                          user be able to change    
                          it?                       

  Q-03                    How many petrol pumps     Defines Places result
                          should be                 handling and UI
                          requested/displayed       pagination.
                          initially? Should         
                          additional results be     
                          loadable?                 

  Q-04                    Should the application    Confirms card content.
                          display estimated driving 
                          time as well as driving   
                          distance? The current     
                          plan includes both, but   
                          distance alone controls   
                          ranking.                  

  Q-05                    Should a station without  Determines destination
                          a DRIVE navigation point  fallback behaviour.
                          be routed using its main  
                          place location, or        
                          excluded from routing?    

  Q-06                    If the only DRIVE         Determines
                          navigation point is       navigation-point
                          marked `PARKING`, may it  selection.
                          be used as a              
                          lower-confidence routing  
                          destination?              

  Q-07                    What is the approved      Google Places response
                          source for washroom       support and data
                          availability? Is          reliability have not
                          `UNKNOWN` acceptable when been confirmed.
                          no source value exists?   

  Q-08                    Does the actual Google    Prevents developers
                          Places response/selected  from guessing fuel
                          field mask return fuel    types.
                          types for your target API 
                          version? If not, what     
                          source should supply      
                          them?                     

  Q-09                    Should Google rating      Finalizes the rating
                          count/review count be     card fields.
                          displayed in addition to  
                          the rating value?         

  Q-10                    The previous BRD included Latest requirements did
                          open/closed status.       not explicitly mention
                          Should it remain in this  it.
                          release, or should it be  
                          removed from the revised  
                          scope?                    

  Q-11                    If Ola Matrix fails for   Defines partial-failure
                          one station, should that  UI and ranking.
                          station be hidden, shown  
                          under an "Distance        
                          unavailable" section, or  
                          handled another way?      

  Q-12                    If two stations have      Defines deterministic
                          equal driving distance,   ranking.
                          what is the tie-breaker:  
                          duration, rating, or no   
                          secondary ordering?       

  Q-13                    What should the side      The specified
                          label mean in the UI:     cross-product algorithm
                          left/right relative to    only supports tentative
                          the vehicle's final       left/right relative to
                          approach, or a stronger   final approach.
                          claim such as             
                          same-side/opposite-side   
                          of a divided road?        

  Q-14                    What test locations or    A numerical tolerance
                          labelled examples can be  must be validated, not
                          used to calibrate the     guessed.
                          side-analysis tolerance   
                          and confidence rules?     

  Q-15                    Should the station action Defines the Maps
                          open the Google Maps      hand-off.
                          place listing or          
                          directions from the       
                          user's origin?            

  Q-16                    Is the first launch       Defines initial launch
                          available wherever Google coverage and testing.
                          Places returns results,   
                          or limited to a selected  
                          city/region?              

  Q-17                    Should the MVP persist    Determines whether a
                          any station/search data,  database is needed in
                          or should each search be  the first release.
                          live and stateless?       

  Q-18                    Is the frontend still     Required to finalize
                          undecided between         frontend structure and
                          Thymeleaf and React       build/deployment steps.
                          bundled/served by Spring  
                          Boot, as stated in the    
                          original BRD?             

  Q-19                    What response-time target Required for
                          should be used for a      performance acceptance
                          normal search?            criteria.

  Q-20                    What browser/device       Required for responsive
                          support is required for   UI and QA acceptance.
                          the first release?        
  -------------------------------------------------------------------------

## 20. Development constraints

-   Treat this document as an updated draft until the open decisions are
    answered.
-   Do not infer washroom availability, fuel types, operating status or
    physical entrance accessibility.
-   Do not replace Ola driving distance with straight-line distance for
    ranking.
-   Do not interpret `LEFT_OF_FINAL_APPROACH` or
    `RIGHT_OF_FINAL_APPROACH` as proof of same-side/opposite-side
    highway access.
-   Do not expose Google or Ola server-side API keys in frontend code.
-   Do not finalize provider field masks, endpoint payloads, batching
    limits or retry values without checking the selected provider API
    documentation and the actual project credentials/plan.
-   Any requirement change must be reflected in this document before it
    is treated as approved for development.

------------------------------------------------------------------------

**Document status:** Updated BRD draft.\
**Next action:** Product owner to answer the open questions, beginning
with Q-01, and approve the resulting development scope.
