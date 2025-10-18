import React, { useEffect, useState } from 'react'
import axios from 'axios'

type Job = {
  id: string
  title: string
  company: string
  location: string
  skills: string[]
  updatedAt: string
  source: string
}

export default function App() {
  const [skills, setSkills] = useState('')
  const [location, setLocation] = useState('')
  const [updatedMinutes, setUpdatedMinutes] = useState(1440)
  const [jobs, setJobs] = useState<Job[]>([])
  const [loading, setLoading] = useState(false)

  async function loadJobs() {
    setLoading(true)
    try {
      const params: any = { updatedSinceMinutes: updatedMinutes }
      if (skills) params.skills = skills
      if (location) params.location = location
      const res = await axios.get('/api/jobs', { params })
      setJobs(res.data)
    } catch (err: any) {
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadJobs() }, [])

  return (
    <div className="container">
      <h1>JobScopeAI — Aggregated Job Search</h1>

      <div style={{ display: 'flex', gap: 8 }}>
        <input placeholder="Skills (comma separated)" value={skills} onChange={e=>setSkills(e.target.value)} />
        <input placeholder="Location" value={location} onChange={e=>setLocation(e.target.value)} />
        <select value={updatedMinutes} onChange={e=>setUpdatedMinutes(Number(e.target.value))}>
          <option value={60}>Last 1 hour</option>
          <option value={360}>Last 6 hours</option>
          <option value={1440}>Last 24 hours</option>
          <option value={10080}>Last 7 days</option>
        </select>
        <button onClick={loadJobs} disabled={loading}>Search</button>
      </div>

      <div style={{ marginTop: 12 }}>
        {loading ? <em>Loading...</em> : (
          jobs.length === 0 ? <div>No results</div> : (
            jobs.map(j => (
              <div key={j.id} style={{ padding: 12, borderBottom: '1px solid #eee' }}>
                <div style={{ fontWeight: 600 }}>{j.title}</div>
                <div style={{ color: '#666' }}>{j.company} — {j.location} <small>({j.source})</small></div>
                <div style={{ marginTop: 6 }}><strong>Skills:</strong> {j.skills.join(', ')}</div>
                <div style={{ marginTop: 6, color: '#888' }}>Updated: {new Date(j.updatedAt).toLocaleString()}</div>
              </div>
            ))
          )
        )}
      </div>
    </div>
  )
}
