# TravelMate accommodation image sources

Last refreshed: 2026-05-28

The demo accommodation images in this folder were refreshed from free photo
libraries requested for the TravelMate presentation dataset. Files are stored
locally so the app does not hotlink third-party images at runtime.

## Source collections

- Pexels house search: <https://www.pexels.com/search/house/>
- Pexels amenities search: <https://www.pexels.com/search/amenities/>
- Unsplash resort search: <https://unsplash.com/fr/s/photos/resort>
- Unsplash villa search: <https://unsplash.com/fr/s/photos/villa>
- Unsplash hotel search: <https://unsplash.com/fr/s/photos/hotel>
- Unsplash homestay search: <https://unsplash.com/fr/s/photos/homestay>
- Unsplash amenities search: <https://unsplash.com/fr/s/photos/amenities>

The iStock and Shutterstock amenity searches below were reviewed as visual
references, but images from those paid stock libraries were not downloaded into
the project to avoid watermarked or license-restricted demo assets:

- Shutterstock gym background search: <https://www.shutterstock.com/vi/search/gym-background>
- iStock bicycle search: <https://www.istockphoto.com/vi/b%E1%BB%A9c-%E1%BA%A3nh/xe-%C4%91%E1%BA%A1p>

## Local mapping

- Detailed file-to-source mapping: `downloaded-image-sources.generated.json`
- Refreshed groups: `hotel`, `lata`, `villa`, `homestay`, `resort`, matching
  catalog images, and accommodation amenity images. The amenity carousel now
  uses separate, content-specific images for hotel, villa, homestay, and resort
  amenities.
- Room detail images ending in `-detail-2` and `-detail-3` are local copies
  generated from the same downloaded Pexels/Unsplash asset pool so every seeded
  room/can has a three-image gallery without runtime hotlinks.

These images are intended for demo/testing. Before production use, review the
current Pexels and Unsplash license pages and attribution preferences.
