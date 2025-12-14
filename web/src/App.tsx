import { Routes, Route } from 'react-router-dom';
import Home from './Home';
import About from './About';
import Privacy from './Privacy';
import './index.css';

import ScrollToTop from './components/ScrollToTop';

function App() {
  return (
    <>
      <ScrollToTop />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/about" element={<About />} />
        <Route path="/privacy" element={<Privacy />} />
        {/* Handle legacy .html URLs if someone manually types them or from old bookmarks */}
        <Route path="/index.html" element={<Home />} />
        <Route path="/about.html" element={<About />} />
        <Route path="/privacy.html" element={<Privacy />} />
      </Routes>
    </>
  );
}

export default App;
