# Keep item photos on the web servers' disk

For whoever changes how larder stores or serves item photos, or moves the web servers. Item photos live on each web
server's disk, copied between the two servers every night, because the volunteers know the disk and photos change
only when the pantry adds a new item.

## Context

In 2024 the pantry's catalogue grew to 600 photos, about 900 MB, and Leo asked where they should live. Volunteers add
perhaps five new items in a week, and a photo almost never changes after it is added. Two forces judge the options:
the volunteers who run larder keep the number of services and accounts they operate down, and a photo added on one
server must reach the other before long.

## Options

- **Object storage**: scales without limit; lost because it adds an account, credentials and a bill for photos that
  fit on the disk many times over.
- **A shared network disk**: one copy; lost because it makes both web servers depend on one more machine.
- **Each web server's own disk**: chosen.

## Decision

Photos are saved on the disk of the web server that received the upload, and a nightly job at 03:00 copies them
to the other server with `rsync`.

## Consequences

A photo added during the day appears on one server only until the nightly copy, so members may see a missing image
on a page for up to a day. A server that is rebuilt gets its photos back from the other server. If the catalogue
grows past a few gigabytes, object storage can be revisited.
