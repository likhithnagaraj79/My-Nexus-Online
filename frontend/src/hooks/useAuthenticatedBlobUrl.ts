import { useEffect, useState } from 'react'
import { apiClient } from '../api/client'

/** Fetches `url` through the authenticated apiClient and exposes it as an object URL.
 * Pass `enabled: false` to defer the request (e.g. until a dialog opens). */
export function useAuthenticatedBlobUrl(url: string | null, enabled = true) {
  const [result, setResult] = useState<{ url: string; blobUrl: string } | null>(null)
  const [failure, setFailure] = useState<{ url: string; error: unknown } | null>(null)

  useEffect(() => {
    if (!url || !enabled) {
      return
    }

    let cancelled = false
    let objectUrl: string | null = null

    apiClient
      .get(url, { responseType: 'blob' })
      .then((response) => {
        if (cancelled) return
        objectUrl = URL.createObjectURL(response.data as Blob)
        setResult({ url, blobUrl: objectUrl })
      })
      .catch((err) => {
        if (!cancelled) setFailure({ url, error: err })
      })

    return () => {
      cancelled = true
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [url, enabled])

  const blobUrl = result && result.url === url ? result.blobUrl : null
  const error = failure && failure.url === url ? failure.error : null
  const loading = enabled && !!url && !blobUrl && !error

  return { blobUrl, error, loading }
}
