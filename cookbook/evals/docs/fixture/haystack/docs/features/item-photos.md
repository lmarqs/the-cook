# Item photos

For whoever adds or changes the photos of pantry items. Each pantry item may have a photo, stored on the web server's
disk under `media/` and resized to 600 px on its longest side. Written when Priya uploaded 6 MB phone pictures that made
the item list take 20 seconds to load on a member's mobile connection.

## Uploading

A volunteer with the admin role attaches a photo on the item's page. Larder accepts JPEG and PNG files up to 8 MB,
resizes the image to 600 px on its longest side and saves it as a JPEG under `media/`, named after the item's id. The
original is discarded, so a volunteer who wants a different crop uploads the picture again.

## Where the files live

The photos are files on the web server's disk, not rows in Postgres, as record 0005 decided. The web app serves
them directly from `media/`. Tom copies `media/` with the rest of the web server's disk in the nightly backup, and an
item without a photo shows a grey placeholder with the item's name.

## What members see

The item list shows each photo at 150 px wide and the item page shows it at 600 px. Every photo carries a short
description written by the volunteer who uploaded it, which screen readers read aloud; Marta checks new descriptions
when she reviews the week's new items.

## Shaped by

- [Keep item photos on the web servers' disk](../adrs/0005-keep-item-photos-on-the-web-servers-disk.md)
