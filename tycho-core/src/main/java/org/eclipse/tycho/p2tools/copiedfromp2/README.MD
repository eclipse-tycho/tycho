# This package contains copied content from P2

To decouple Tycho from P2 and apply some changes to behavior of internal classes we have copies of these here, the mostly differ in some details, e.g. making methods or fields protected.
In the (unlikely) case something changes in P2 one should be able to easily sync the changes by copy over the new version and perform a git compare to apply the patch from P2 to Tycho as well.

These are the original location in P2:

- AbstractApplication -> org.eclipse.equinox.p2.internal.repository.tools.AbstractApplication
- MirrorApplication -> org.eclipse.equinox.p2.internal.repository.tools.MirrorApplication

## History

Tycho already customizes the P2 applications but they are not really used as a real application and instead called directly. In the past especially the MirrorApplication required some adjustments to fit Tycho needs, but it was not written with extensibility in mind.

Even though sometimes we we able to work around that limitations or apply patches to P2 this really has slowed down the development and ability of Tycho to deliver new value to their users. On the other hand none of these changes has had added any value to pure P2 users and there are no real innovation in these areas over the years.

Because of this, Tycho now includes a copy of these classes (but remains the extension pattern to allow apply of P2 patches to Tycho) to fasten development and decouple Tycho form P2 in the area of tools.
