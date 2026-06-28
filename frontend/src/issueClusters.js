import api from './api'

const statusRank = { IN_PROGRESS: 5, ESCALATED: 4, VERIFIED: 3, REPORTED: 2, RESOLVED: 1 }
const exactDuplicateMeters = 80
const nearbyDuplicateMeters = 500
const sameAreaTextThreshold = 0.12

export async function clusterPublicIssues(issues) {
  const byId = new Map(issues.map(issue => [issue.id, issue]))
  const parent = new Map(issues.map(issue => [issue.id, issue.id]))

  const find = (id) => {
    const current = parent.get(id)
    if (current === id) return id
    const root = find(current)
    parent.set(id, root)
    return root
  }

  const union = (left, right) => {
    const leftRoot = find(left)
    const rightRoot = find(right)
    if (leftRoot !== rightRoot) parent.set(rightRoot, leftRoot)
  }

  await Promise.all(issues.map(async issue => {
    try {
      const { data } = await api.get(`/issues/${issue.id}/duplicates`)
      data.forEach(candidate => {
        if (byId.has(candidate.id)) union(issue.id, candidate.id)
      })
    } catch {
      // Duplicate clustering is a display enhancement. Keep issues visible if it fails.
    }
  }))

  for (let leftIndex = 0; leftIndex < issues.length; leftIndex += 1) {
    for (let rightIndex = leftIndex + 1; rightIndex < issues.length; rightIndex += 1) {
      if (looksLikeSameCivicCase(issues[leftIndex], issues[rightIndex])) {
        union(issues[leftIndex].id, issues[rightIndex].id)
      }
    }
  }

  const groups = new Map()
  issues.forEach(issue => {
    const root = find(issue.id)
    groups.set(root, [...(groups.get(root) || []), issue])
  })

  return [...groups.values()].map(group => {
    const primary = [...group].sort(comparePrimaryIssues)[0]
    const firstWithMedia = [...group].sort((a, b) => a.id - b.id).find(issue => issue.media?.some(item => item.mediaType === 'IMAGE'))
    return {
      ...primary,
      media: firstWithMedia?.media || primary.media,
      description: combinedDescription(group, primary),
      duplicateReportCount: group.length,
      duplicateIssueIds: group.filter(issue => issue.id !== primary.id).map(issue => issue.id),
    }
  })
}

export function comparePrimaryIssues(left, right) {
  const statusDifference = (statusRank[right.status] || 0) - (statusRank[left.status] || 0)
  if (statusDifference) return statusDifference

  const verificationDifference = (right.verificationCount || 0) - (left.verificationCount || 0)
  if (verificationDifference) return verificationDifference

  const impactDifference = (right.impactScore || 0) - (left.impactScore || 0)
  if (impactDifference) return impactDifference

  return left.id - right.id
}

function looksLikeSameCivicCase(left, right) {
  if (!left || !right || left.id === right.id) return false
  if (left.category !== right.category) return false

  const distance = distanceMeters(left.latitude, left.longitude, right.latitude, right.longitude)
  const sameLocality = normalized(left.locality) && normalized(left.locality) === normalized(right.locality)
    && normalized(left.city) === normalized(right.city)
    && normalized(left.state) === normalized(right.state)
  const similarity = textSimilarity(`${left.title} ${left.description}`, `${right.title} ${right.description}`)

  return distance <= exactDuplicateMeters
    || (distance <= nearbyDuplicateMeters && similarity >= sameAreaTextThreshold)
    || (sameLocality && similarity >= sameAreaTextThreshold)
}

function combinedDescription(group, primary) {
  if (group.length === 1) return primary.description
  const descriptions = [...group]
    .sort((a, b) => a.id - b.id)
    .map(issue => issue.description?.trim())
    .filter(Boolean)
    .filter((description, index, all) => all.indexOf(description) === index)
  if (descriptions.length <= 1) return primary.description
  return `${group.length} citizen reports describe this same issue: ${descriptions.join(' Also reported: ')}`
}

function normalized(value) {
  return (value || '').trim().toLowerCase()
}

function textSimilarity(left, right) {
  const leftTokens = tokens(left)
  const rightTokens = tokens(right)
  if (!leftTokens.size || !rightTokens.size) return 0
  const intersection = [...leftTokens].filter(token => rightTokens.has(token)).length
  const union = new Set([...leftTokens, ...rightTokens]).size
  return intersection / union
}

function tokens(value) {
  const stopWords = new Set(['the', 'and', 'for', 'with', 'this', 'that', 'near', 'from', 'has', 'can', 'got'])
  return new Set(String(value || '')
    .toLowerCase()
    .replace(/[^a-z0-9 ]/g, ' ')
    .split(/\s+/)
    .filter(token => token.length >= 3 && !stopWords.has(token)))
}

function distanceMeters(lat1, lon1, lat2, lon2) {
  const firstLat = Number(lat1)
  const firstLon = Number(lon1)
  const secondLat = Number(lat2)
  const secondLon = Number(lon2)
  if (![firstLat, firstLon, secondLat, secondLon].every(Number.isFinite)) return Number.POSITIVE_INFINITY

  const latDistance = radians(secondLat - firstLat)
  const lonDistance = radians(secondLon - firstLon)
  const a = Math.sin(latDistance / 2) ** 2
    + Math.cos(radians(firstLat)) * Math.cos(radians(secondLat)) * Math.sin(lonDistance / 2) ** 2
  return 6371000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

function radians(value) {
  return value * Math.PI / 180
}
