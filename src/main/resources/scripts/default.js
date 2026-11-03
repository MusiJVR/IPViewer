/**
 * Called asynchronously by IPViewer with the player's IP address.
 * Return an object (or JSON string) with string fields "country" and "state".
 * httpGet(url, headers?) is provided by IPViewer and returns the response body.
 * It throws on network errors or non-2xx responses; HTTP timeout is 5 seconds.
 * This is a synchronous function: do not use async/await or return a Promise.
 * Changes in plugins/IPViewer/scripts apply on the next join.
 * Provider documentation and free-tier limits: https://ipwhois.io/documentation
 */
function getGeoLocation(ip) {
  const url = `https://ipwho.is/${encodeURIComponent(ip)}?fields=success,message,country,region`
  const data = JSON.parse(httpGet(url, {Accept: "application/json"}))

  if (data.success !== true)
    throw new Error("GeoIP service error: " + (data.message || "Unknown error"))

  if (typeof data.country !== "string" || typeof data.region !== "string")
    throw new Error("GeoIP service returned invalid country or region")

  return {
    country: data.country,
    state: data.region
  }
}
