# Foreman product marks

Foreman's canonical product mark is the branching **F** on the purple rounded
square. Product identity and connection state are separate: a green presence
dot may be placed beside the mark, but is never baked into the logo artwork.

## Approved variants

| Variant | Source asset | Use |
| --- | --- | --- |
| Full-color master | `android/app/src/main/res/drawable-nodpi/foreman_logo.png` | Android launcher, splash, header, setup, pairing, and About surfaces |
| Full-color web copy | `web/public/foreman-logo.png` | Exact copy of the master for browser chrome and every web product-logo surface through `ForemanLogo` |
| Compact monochrome | `android/app/src/main/res/drawable/ic_notification.xml` | Android status-bar and notification small icons only |

The compact notification mark is a solid silhouette of the **F** implied by
the branching logo. Android supplies its color; it deliberately omits the
purple tile, gradients, fine branch nodes, and presence state so it stays
legible at status-bar size.

Do not draw substitute letter avatars inside individual components. Reuse the
approved platform asset, keep its aspect ratio, and render a connection dot as
a separate semantic element when the surface needs one. A regression test
keeps the web copy byte-for-byte identical to the Android master.
